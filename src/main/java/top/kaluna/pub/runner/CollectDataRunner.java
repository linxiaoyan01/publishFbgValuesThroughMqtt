package top.kaluna.pub.runner;
/*
 * Copyright 2016 Kevin Herron
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import io.netty.util.ReferenceCountUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import io.netty.buffer.ByteBufUtil;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import top.kaluna.pub.domain.FbgValue;
import top.kaluna.pub.mapper.FbgValueInfoMapper;
import top.kaluna.pub.mapper.FbgValueMapper;
import top.kaluna.pub.service.WsService;
import top.kaluna.pub.util.CommonUtil;
import javax.annotation.Resource;

/**
 * @author 86158
 */
@Component
@Order(1)
public class CollectDataRunner implements ApplicationRunner {
    @Resource
    private WsService wsService;
    @Autowired
    private FbgValueMapper fbgValueMapper;
    @Autowired
    private FbgValueInfoMapper fbgValueInfoMapper;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<ModbusTcpMaster> masters = new CopyOnWriteArrayList<>();
    private volatile boolean started = false;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static int TOTALPHYSICVAL = 6;


    public void start() throws InterruptedException, MqttException {
        started = true;

        ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder("192.168.1.100")
                .setPort(502)
                .build();
        ModbusTcpMaster master = new ModbusTcpMaster(config);
        master.connect();

        String userName = "strain";
        String passWord = "strain";
        MqttClient mqttClient = Publish.connect(userName, passWord);

        Thread thread = new Thread(() -> {
            while (started) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sendAndReceive(master, mqttClient);
            }
        });
        thread.start();
    }

    private void sendAndReceive(ModbusTcpMaster master, MqttClient mqttClient) {
        if (!started){
            return;
        }
        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(0, TOTALPHYSICVAL * 2), 1);
        future.whenCompleteAsync((response, ex) -> {
            if (response != null) {
                System.out.println("Response: "+ ByteBufUtil.hexDump(response.getRegisters()));

                byte[][] bytes = new byte[TOTALPHYSICVAL][4];
                int[] physicValueInt = new int[TOTALPHYSICVAL];
                Float[] physicValueFloat = new Float[TOTALPHYSICVAL];
                //Map<String, Float> map = new HashMap<>(6);
                List<FbgValue> fbgValues = new ArrayList<>();
                //将解析出来的数据设置到physicValueFloat
                for(int i = 0; i < TOTALPHYSICVAL; i++){
                    bytes[i] = ByteBufUtil.getBytes(response.getRegisters(),i*4,4);
                    //System.out.println("第"+(i+1)+"个"+"物理值: "+ByteBufUtil.hexDump(response.getRegisters(),i*4,4));
                    physicValueInt[i] = CommonUtil.bytesToInt2(bytes[i],0);
                    //System.out.println("第"+(i+1)+"个"+"物理值(转成十进制): "+ physicValue[i]);
                    physicValueFloat[i] = (float)physicValueInt[i]/(float) 10000;
                    System.out.println("第"+(i+1)+"个"+"物理值(转成十进制，除以10000): "+ physicValueFloat[i]);
                    //map.put("第"+(i+1)+"个"+"物理值(转成十进制，除以10000): ", physicValueFloat[i]);
                }

                setSixValueAndTag(physicValueFloat,fbgValues);
                //存储到数据库
                fbgValueMapper.multipleInsert(fbgValues);
                //发送数据给mqtt服务器
                try {
                    //这此runner发送给mqtt服务器的主题为应变值，其他runner就改变主题即可区分是什么传感器发来的消息
                    Publish.publish(fbgValues.toString(),mqttClient,"/tcp/strain");
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                ReferenceCountUtil.release(response);
            } else {
                logger.error("Completed exceptionally, message={}", ex.getMessage(), ex);
            }
        }, scheduler);
    }

    /**
     * 只能知道第一个断裂的位置 后面的若断裂了会通过定时任务扫描发现
     * 目前做不到所有种类的传感器都一起采集。。。。。。。。。怎么办？
     * 开启多个runner？用其他runner去采集其他光纤光栅解调仪的其他传感器数据？或许吧
     * 如果是这样，比如采集振动值，必须在另一个runner中给fbgValues设置PhysicalValueInfoId的时候，for循环中要给i手动加上偏移值
     * 多个runner同时给mqtt发送消息，也就是说发布者有多个，那只有一台接收者是不太靠谱的，有可能要考虑负载均衡
     * 按理来说客户端只需要关心负载均衡的地址，不需要知道集群内各个节点的地址。这个以后再讨论
     * mqtt订阅者（也就是我的部署在ecs服务器上的程序，ecs已经启动了broker）收到不同主题的消息，websocket推送，大概是这样的逻辑
     * @param physicValueFloat 传感器实时监听到的应变值
     * @param fbgValues 数据库中fbgValues的映射
     */
    public void setSixValueAndTag(Float[] physicValueFloat, List<FbgValue> fbgValues){
        Date date = new Date();
        byte tag = 0;
        for (int i = 0; i < TOTALPHYSICVAL; i++){
            if (physicValueFloat[i] == null && tag == 0){
                tag = (byte) (i+1);
            }else{
                tag = 0;
            }
            fbgValues.add(FbgValue.Builder.builder()
                    .withPhysicalValueInfoId((long) (i+1))
                    .withValue(new BigDecimal(Float.toString(physicValueFloat[i]==null ? 0 : physicValueFloat[i])))
                    .withTag(tag)
                    .withCreateTime(date.getTime()).build());
        }
    }
    public void stop() {
        started = false;
        masters.forEach(ModbusTcpMaster::disconnect);
        masters.clear();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        //目的是为了给 TOTALPHYSICVAL 赋值
        getTotal();
        //启动线程采集并发送给mqtt服务器
        start();
    }

    private void getTotal() {
        TOTALPHYSICVAL = fbgValueInfoMapper.total();
    }
}