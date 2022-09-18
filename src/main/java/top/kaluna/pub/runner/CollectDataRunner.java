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

import com.alibaba.fastjson.JSONObject;
import com.digitalpetri.modbus.codec.Modbus;
import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.ReferenceCountUtil;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.kaluna.pub.connect.Publish;
import top.kaluna.pub.domain.FbgValue;
import top.kaluna.pub.domain.FbgValueInfo;
import top.kaluna.pub.mapper.FbgValueInfoMapper;
import top.kaluna.pub.mapper.FbgValueMapper;
import top.kaluna.pub.util.CommonUtil;
import top.kaluna.pub.util.SnowFlake;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author 86158
 */
@Component
@Order(1)
public class CollectDataRunner implements ApplicationRunner {

    @Resource
    private FbgValueInfoMapper fbgValueInfoMapper;

    @Resource
    private FbgValueMapper fbgValueMapper;

    private final List<ModbusTcpMaster> masters = new CopyOnWriteArrayList<>();
    private volatile boolean started = false;
    private final Logger LOG = LoggerFactory.getLogger(CollectDataRunner.class);
    @Resource
    private SnowFlake snowFlake;

    static ModbusTcpMaster master;

    static class ChannelAndArrayNum{
        private Integer channel;
        private Integer arrayNum;

        public ChannelAndArrayNum(){

        }
        public ChannelAndArrayNum(Integer channel, Integer arrayNum){
            this.channel = channel;
            this.arrayNum = arrayNum;
        }
        public Integer getChannel() {
            return channel;
        }

        public void setChannel(Integer channel) {
            this.channel = channel;
        }

        public Integer getArrayNum() {
            return arrayNum;
        }

        public void setArrayNum(Integer arrayNum) {
            this.arrayNum = arrayNum;
        }
    }
    @Override
    public void run(ApplicationArguments args) throws Exception {
        //linkedHashMap store each channel(key) and its total arrayNum(value)
        final LinkedHashMap<Integer, Integer> channelAndTotalArrayNum = getChannelAndTotalArrayNum();
        int totalSensorNum = 0;
        for(int key : channelAndTotalArrayNum.keySet()){
            totalSensorNum += channelAndTotalArrayNum.get(key);
        }
        ChannelAndArrayNum [] channelAndArrayNum = new ChannelAndArrayNum[totalSensorNum];
        final Set<Integer> integers = channelAndTotalArrayNum.keySet();
        int count = 0;
        for(int i = 0; i < channelAndTotalArrayNum.size(); i++){
            for(int j = 0, key = integers.stream().iterator().next(), value = channelAndTotalArrayNum.get(key); j < value; j++){
                channelAndArrayNum[count].setChannel(key);
                channelAndArrayNum[count].setArrayNum(j+1);
                count++;
            }
        }

        //启动线程采集
        Runnable runnable = () -> {
            try {
                final ModbusTcpMaster modbusTcpMaster = initModbusTcpMaster();
                final List list = startReadDifferentChannel(modbusTcpMaster, channelAndTotalArrayNum);
                final List<Float[]> floats = (List<Float[]>) list.get(0);
                //final ArrayList allArrayNum = (ArrayList) list.get(1);
                //所有传感阵列的数据
                List<List<FbgValue>> fbgValuess = new ArrayList<>();
                //每隔10分钟一次，存储到数据库
                //Calendar cal = Calendar.getInstance();
                //int now = cal.get(Calendar.MINUTE);

                for (int i = 0, floatsSize = floats.size(); i < floatsSize; i++) {
                    //阵列的数据
                    Float[] aFloat = floats.get(i);
                    //断点的位置
                    //int arrayNum = (int) allArrayNum.get(i);
                    //每个传感阵列的所有数据
                    List<FbgValue> fbgValues = new ArrayList<>();
                    //setValueAndTag(aFloat, fbgValues, arrayNum,i+1);
                    setValueAndTag(aFloat, fbgValues,channelAndArrayNum[i].getChannel(), channelAndArrayNum[i].getArrayNum());
                    //每隔10分钟一次，存储到数据库
                    //if(now % 10 == 0){
                        //saveToMySQL(fbgValues);
                    //}
                    fbgValuess.add(fbgValues);
                }
                sendToMQTT(fbgValuess);
                modbusTcpMaster.disconnect();
            } catch (ExecutionException | MqttException | InterruptedException | UnknownHostException e) {
                e.printStackTrace();
            }
        };
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, 1, 1, TimeUnit.SECONDS);
    }
    /**
     * 获取TCP协议的Master
     * @return ModbusTcpMaster
     */
    public static ModbusTcpMaster initModbusTcpMaster() {
        if (master == null) {
            // 创建配置
            ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder("192.168.1.100").setPort(502).build();
            master = new ModbusTcpMaster(config);
        }
        return master;
    }
    /**
     * 这个函数是采集10个不同通道的传感器数据
     */
    public List startReadDifferentChannel(ModbusTcpMaster master, LinkedHashMap<Integer, Integer> channelAndTotalArrayNum) throws ExecutionException, InterruptedException, MqttException, UnknownHostException {
        master.connect();
        //所有通道的数据
        List<Float[]> allCollectData = new ArrayList<>();
        //所有断点的位置列表
        //List arrayNumList = new ArrayList();
        List allCollectDataAndAllArrayNum = new ArrayList();

        for(int key : channelAndTotalArrayNum.keySet()){
            final List list = readHoldingRegisterr(master, (key - 1) * 48, channelAndTotalArrayNum.get(key), 1);
            allCollectData.add((Float[])list.get(0));
        }
        allCollectDataAndAllArrayNum.add(allCollectData);
        //allCollectDataAndAllArrayNum.add(arrayNumList);
        return allCollectDataAndAllArrayNum;
    }

    /**
     * 采集一个通道下的阵列的所有数据
     * @param master 主机
     * @param address 起始地址，这里不同通道的地址计算是比如00201 (2-1)*48+(1-1)*2=48
     * @param sensorNum 这个通道的传感器数量
     * @param unitId 从站ID
     * @return 返回这个通道读取到的数据和断点位置
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static List readHoldingRegisterr(ModbusTcpMaster master, int address, int sensorNum, int unitId){
        CompletableFuture<ReadHoldingRegistersResponse> future = master.sendRequest(new ReadHoldingRegistersRequest(address,sensorNum*2),unitId);
        ReadHoldingRegistersResponse response = null;
        try {
            response = future.get();
        } catch (InterruptedException | ExecutionException e) {
            System.out.println(e.toString());
        }
        byte[][] bytes = new byte[sensorNum][4];
        int[] physicValueInt = new int[sensorNum];
        Float[] physicValueFloat = new Float[sensorNum];
        //定义一个变量存储断点的阵列序号 0表示没有断点 1表示第一个光纤光栅断裂
        if(response != null){
            //读取这个通道下阵列的所有数据
            for(int i = 0; i < sensorNum; i++){
                bytes[i] = ByteBufUtil.getBytes(response.getRegisters(),i*4,4);
                physicValueInt[i] = CommonUtil.bytesToInt2(bytes[i],0);
                physicValueFloat[i] = (float)physicValueInt[i]/(float) 10000;
                //得到断点的位置
//                if(arrayNum == 0 && (physicValueFloat[i] == 0.0 || physicValueFloat[i] == null)){
//                    arrayNum = i+1;
//                }else{
//                    arrayNum = 0;
//                }
                System.out.println("物理值(转成十进制，除以10000): "+ physicValueFloat[i]);
            }
        };
        ReferenceCountUtil.release(response);
        List resultAndArrayNumResult = new ArrayList();
        resultAndArrayNumResult.add(physicValueFloat);
        //record the breakpoint
        //resultAndArrayNumResult.add(arrayNum);
        return resultAndArrayNumResult;
    }



    /**
     * @param physicValueFloat 传感器实时监听到的应变值
     * @param fbgValues 数据库中fbgValues的映射
     * @param arrayNum 断点位置
     */
    //
    public void setValueAndTag(Float[] physicValueFloat, List<FbgValue> fbgValues, int channel, int arrayNum) {
        Date date = new Date();
        for (int i = 0; i < physicValueFloat.length; i++) {
            fbgValues.add(FbgValue.Builder.builder()
                    .withPhysicalValueInfoId((long) (i + 1))
                    .withValue(new BigDecimal(Float.toString(physicValueFloat[i])))
                    .withArrayNum(arrayNum)
                    .withChannel(channel)
                    .withCreateTime(date.getTime()).build());
        }
    }

    public void stop() {
        started = false;
        masters.forEach(ModbusTcpMaster::disconnect);
        masters.clear();
    }
    /***
     * 释放资源
     */
    public static void release(ModbusTcpMaster master) {
        if (master != null) {
            master.disconnect();
        }
        Modbus.releaseSharedResources();
    }
    private void saveToMySQL(List<FbgValue> fbgValues) {
        fbgValueMapper.multipleInsert(fbgValues);
    }


    @Async
    void sendToMQTT(List<List<FbgValue>> fbgValuess) throws MqttException {
        //发送给mqtt服务器
        String userName = "fbg";
        String passWord = "fbg";
        MqttClient mqttClient = Publish.connect(userName, passWord);

        List<FbgValue> allCollectData = new ArrayList<>();
        fbgValuess.forEach((obj)->{
            allCollectData.addAll(obj);
        });
//        for (List<FbgValue> valuess : fbgValuess) {
//            strain.add(valuess.get(0));
//        }
//        classified.add(strain);
        //增加日志流水号
        MDC.put("LOG_ID",String.valueOf(snowFlake.nextId()));
        String logId = MDC.get("LOG_ID");
        LOG.info("推送新消息 "+logId);
        long start = System.currentTimeMillis();
        try {
            Publish.publish(JSONObject.toJSONString(allCollectData),mqttClient,"/tcp/fbg");
        } catch (MqttException e) {
            e.printStackTrace();
        }
        LOG.info("推送新消息结束，耗时：{}毫秒",System.currentTimeMillis() - start);
    }
    
    //在读取传感器数据之前，需要知道每个传感器的通道号channel和阵列号arrayNum，查询数据库
    private LinkedHashMap<Integer, Integer> getChannelAndTotalArrayNum() {
        final List<FbgValueInfo> fbgValueInfos = fbgValueInfoMapper.selectAllRecord();
        final List<FbgValueInfo> fbgValueInfos1 = fbgValueInfos.stream().sorted(Comparator.comparing(FbgValueInfo::getArrayNum)).collect(Collectors.toList());
        final List<FbgValueInfo> fbgValueInfos2 = fbgValueInfos1.stream().sorted(Comparator.comparing(FbgValueInfo::getChannel)).collect(Collectors.toList());
        LinkedHashMap<Integer, Integer> linkedHashMap = new LinkedHashMap<>();
        final LinkedHashMap<Integer, List<FbgValueInfo>> collect = fbgValueInfos2.stream().collect(Collectors.groupingBy(FbgValueInfo::getChannel, LinkedHashMap::new, Collectors.toList()));
        for(int key : collect.keySet()){
            linkedHashMap.put(key, collect.get(key).size());
        }
        return linkedHashMap;
    }
}