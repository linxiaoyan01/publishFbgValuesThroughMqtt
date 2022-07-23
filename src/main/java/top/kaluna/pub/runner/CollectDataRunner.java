//package top.kaluna.pub.runner;
///*
// * Copyright 2016 Kevin Herron
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//import com.alibaba.fastjson.JSONObject;
//import com.digitalpetri.modbus.codec.Modbus;
//import com.digitalpetri.modbus.master.ModbusTcpMaster;
//import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
//import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
//import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
//import io.netty.buffer.ByteBufUtil;
//import io.netty.util.ReferenceCountUtil;
//import org.eclipse.paho.client.mqttv3.MqttClient;
//import org.eclipse.paho.client.mqttv3.MqttException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.slf4j.MDC;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//import top.kaluna.pub.connect.Publish;
//import top.kaluna.pub.domain.FbgValue;
//import top.kaluna.pub.mapper.FbgValueInfoMapper;
//import top.kaluna.pub.mapper.FbgValueMapper;
//import top.kaluna.pub.util.CommonUtil;
//import top.kaluna.pub.util.SnowFlake;
//
//import javax.annotation.Resource;
//import java.math.BigDecimal;
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.List;
//import java.util.concurrent.*;
//
///**
// * @author 86158
// */
//@Component
//@Order(1)
//public class CollectDataRunner implements ApplicationRunner {
//
//    @Resource
//    private FbgValueInfoMapper fbgValueInfoMapper;
//
//    @Resource
//    private FbgValueMapper fbgValueMapper;
//
//    private final List<ModbusTcpMaster> masters = new CopyOnWriteArrayList<>();
//    private volatile boolean started = false;
//
//    private static int TOTALPHYSICVAL;
//
//    private final Logger LOG = LoggerFactory.getLogger(CollectDataRunner.class);
//    @Resource
//    private SnowFlake snowFlake;
//
//    static ModbusTcpMaster master;
//
//    @Override
//    public void run(ApplicationArguments args) throws Exception {
//
//        //目的是为了给 TOTALPHYSICVAL 赋值 查看应变传感器的个数
//        getTotal();
//        //启动线程采集
//        Runnable runnable = () -> {
//            try {
//                final ModbusTcpMaster modbusTcpMaster = initModbusTcpMaster();
//                final List list = startReadDifferentChannel(modbusTcpMaster);
//                final List<Float[]> floats = (List<Float[]>) list.get(0);
//                final ArrayList allArrayNum = (ArrayList) list.get(1);
//                //所有传感阵列的数据
//                List<List<FbgValue>> fbgValuess = new ArrayList<>();
//                //每隔10分钟一次，存储到数据库
//                Calendar cal = Calendar.getInstance();
//                int now = cal.get(Calendar.MINUTE);
//
//                for (int i = 0, floatsSize = floats.size(); i < floatsSize; i++) {
//                    //阵列的数据
//                    Float[] aFloat = floats.get(i);
//                    //断点的位置
//                    int arrayNum = (int) allArrayNum.get(i);
//                    //每个传感阵列的所有数据
//                    List<FbgValue> fbgValues = new ArrayList<>();
//                    setValueAndTag(aFloat, fbgValues, arrayNum,i+1);
//                    //每隔10分钟一次，存储到数据库
//                    if(now % 10 == 0){
//                        saveToMySQL(fbgValues);
//                    }
//                    fbgValuess.add(fbgValues);
//                }
//                sendToMQTT(fbgValuess);
//                modbusTcpMaster.disconnect();
//            } catch (ExecutionException | MqttException | InterruptedException | UnknownHostException e) {
//                e.printStackTrace();
//            }
//        };
//        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
//        service.scheduleAtFixedRate(runnable, 1, 1, TimeUnit.SECONDS);
//    }
//    /**
//     * 获取TCP协议的Master
//     *
//     * @return
//     */
//    public static ModbusTcpMaster initModbusTcpMaster() {
//        if (master == null) {
//            // 创建配置
//            ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder("192.168.1.100").setPort(502).build();
//            master = new ModbusTcpMaster(config);
//        }
//        return master;
//    }
//
//
//    /**
//     * 这个函数是采集10个不同通道的传感器数据
//     */
//    public List startReadDifferentChannel(ModbusTcpMaster master) throws ExecutionException, InterruptedException, MqttException, UnknownHostException {
//        master.connect();
//        //所有通道的数据
//        List<Float[]> allCollectData = new ArrayList<>();
//        //所有断点的位置列表
//        List arrayNumList = new ArrayList();
//        List allCollectDataAndAllArrayNum = new ArrayList();
//
//        //i+1指的是通道号
//        for(int i = 0; i < TOTALPHYSICVAL; i++){
//            final List list = readHoldingRegisterr(master, i * 48, 1, 1);
//            allCollectData.add((Float[]) list.get(0));
//            arrayNumList.add(list.get(1));
//        }
//        allCollectDataAndAllArrayNum.add(allCollectData);
//        allCollectDataAndAllArrayNum.add(arrayNumList);
//        return allCollectDataAndAllArrayNum;
//    }
//
//
////    public void startReadTheSameChannel() throws InterruptedException, MqttException, ExecutionException {
////        started = true;
////
////        ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder("192.168.1.100")
////                .setPort(502)
////                .build();
////        ModbusTcpMaster master = new ModbusTcpMaster(config);
////        master.connect();
////
////        String userName = "strain";
////        String passWord = "strain";
////        MqttClient mqttClient = Publish.connect(userName, passWord);
////        Thread thread = new Thread(() -> {
////            while (started) {
////                try {
////                    Thread.sleep(1000);
////                } catch (InterruptedException e) {
////                    e.printStackTrace();
////                }
////                sendAndReceive(master, mqttClient);
////            }
////        });
////        thread.start();
////
//
//    /**
//     * 采集一个通道下的阵列的所有数据
//     * @param master 主机
//     * @param address 起始地址，这里不同通道的地址计算是比如00201 (2-1)*48+(1-1)*2=48
//     * @param sensorNum 这个通道的传感器数量
//     * @param unitId 从站ID
//     * @return 返回这个通道读取到的数据和断点位置
//     * @throws ExecutionException
//     * @throws InterruptedException
//     */
//    public static List readHoldingRegisterr(ModbusTcpMaster master, int address, int sensorNum, int unitId){
//        CompletableFuture<ReadHoldingRegistersResponse> future = master.sendRequest(new ReadHoldingRegistersRequest(address,sensorNum*2),unitId);
//        ReadHoldingRegistersResponse response = null;
//        try {
//            response = future.get();
//        } catch (InterruptedException | ExecutionException e) {
//            System.out.println(e.toString());
//        }
//        byte[][] bytes = new byte[sensorNum][4];
//        int[] physicValueInt = new int[sensorNum];
//        Float[] physicValueFloat = new Float[sensorNum];
//        //定义一个变量存储断点的阵列序号 0表示没有断点 1表示第一个光纤光栅断裂
//        int arrayNum = 0;
//        if(response != null){
//            //读取这个通道下阵列的所有数据
//            for(int i = 0; i < sensorNum; i++){
//                bytes[i] = ByteBufUtil.getBytes(response.getRegisters(),i*4,4);
//                physicValueInt[i] = CommonUtil.bytesToInt2(bytes[i],0);
//                physicValueFloat[i] = (float)physicValueInt[i]/(float) 10000;
//                //得到断点的位置
//                if(arrayNum == 0 && (physicValueFloat[i] == 0.0 || physicValueFloat[i] == null)){
//                    arrayNum = i+1;
//                }else{
//                    arrayNum = 0;
//                }
//                System.out.println("物理值(转成十进制，除以10000): "+ physicValueFloat[i]);
//            }
//        };
//        ReferenceCountUtil.release(response);
//        List resultAndArrayNumResult = new ArrayList();
//        resultAndArrayNumResult.add(physicValueFloat);
//        resultAndArrayNumResult.add(arrayNum);
//        return resultAndArrayNumResult;
//    }
//
//
//
//    /**
//     * 只能知道第一个断裂的位置 后面的若断裂了会通过定时任务扫描发现
//     * 目前做不到所有种类的传感器都一起采集。。。。。。。。。怎么办？
//     * 开启多个runner？用其他runner去采集其他光纤光栅解调仪的其他传感器数据？或许吧
//     * 如果是这样，比如采集振动值，必须在另一个runner中给fbgValues设置PhysicalValueInfoId的时候，for循环中要给i手动加上偏移值
//     * 多个runner同时给mqtt发送消息，也就是说发布者有多个，那只有一台接收者是不太靠谱的，有可能要考虑负载均衡
//     * 按理来说客户端只需要关心负载均衡的地址，不需要知道集群内各个节点的地址。这个以后再讨论
//     * mqtt订阅者（也就是我的部署在ecs服务器上的程序，ecs已经启动了broker）收到不同主题的消息，websocket推送，大概是这样的逻辑
//     * @param physicValueFloat 传感器实时监听到的应变值
//     * @param fbgValues 数据库中fbgValues的映射
//     * @param arrayNum 断点位置
//     */
//    //
//    public void setValueAndTag(Float[] physicValueFloat, List<FbgValue> fbgValues, int arrayNum, int channel){
//        Date date = new Date();
//        for (int i = 0; i < physicValueFloat.length; i++){
//            fbgValues.add(FbgValue.Builder.builder()
//                    .withPhysicalValueInfoId((long) (i+1))
//                    .withValue(new BigDecimal(Float.toString(physicValueFloat[i]==null ? 0 : physicValueFloat[i])))
//                    .withArrayNum(arrayNum)
//                    .withChannel(channel)
//                    .withCreateTime(date.getTime()).build());
//        }
//    }
//
//    public void stop() {
//        started = false;
//        masters.forEach(ModbusTcpMaster::disconnect);
//        masters.clear();
//    }
//    /***
//     * 释放资源
//     */
//    public static void release(ModbusTcpMaster master) {
//        if (master != null) {
//            master.disconnect();
//        }
//        Modbus.releaseSharedResources();
//    }
//
//
//
//    private void saveToMySQL(List<FbgValue> fbgValues) {
//        fbgValueMapper.multipleInsert(fbgValues);
//    }
//
//
//    @Async
//    void sendToMQTT(List<List<FbgValue>> fbgValuess) throws MqttException {
//        //发送给mqtt服务器
//        String userName = "strain";
//        String passWord = "strain";
//        MqttClient mqttClient = Publish.connect(userName, passWord);
//
//        //由于我知道哪些是应变，哪些是振动，哪些是温度，所以可以提前分类
//        List<List<FbgValue>> classified = new ArrayList<>(3);
//        List<FbgValue> strain = new ArrayList<>();
//        for (List<FbgValue> valuess : fbgValuess) {
//            strain.add(valuess.get(0));
//        }
//        classified.add(strain);
//        //增加日志流水号
//        MDC.put("LOG_ID",String.valueOf(snowFlake.nextId()));
//        String logId = MDC.get("LOG_ID");
//        LOG.info("推送新消息 "+logId);
//        long start = System.currentTimeMillis();
//        try {
//            //这此runner发送给mqtt服务器的主题为应变值，其他runner就改变主题即可区分是什么传感器发来的消息
//            Publish.publish(JSONObject.toJSONString(classified),mqttClient,"/tcp/strain");
//            //System.out.println(JSONObject.toJSONString(fbgValuess));
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }
//        LOG.info("推送新消息结束，耗时：{}毫秒",System.currentTimeMillis() - start);
//    }
//
//    private void getTotal() {
//        TOTALPHYSICVAL = fbgValueInfoMapper.strainTotal();
//        //System.out.println(TOTALPHYSICVAL);
//    }
//
//    //    @Deprecated
////    private void sendAndReceive(ModbusTcpMaster master, MqttClient mqttClient) {
////        if (!started){
////            return;
////        }
////        CompletableFuture<ReadHoldingRegistersResponse> future1 =
////                master.sendRequest(new ReadHoldingRegistersRequest(0, TOTALPHYSICVAL*2), 1);
////        future1.whenCompleteAsync((response, ex) -> {
////            if (response != null) {
////                System.out.println("Response: "+ ByteBufUtil.hexDump(response.getRegisters()));
////                byte[][] bytes = new byte[TOTALPHYSICVAL][4];
////                int[] physicValueInt = new int[TOTALPHYSICVAL];
////                Float[] physicValueFloat = new Float[TOTALPHYSICVAL];
////                //Map<String, Float> map = new HashMap<>(6);
////                List<FbgValue> fbgValues = new ArrayList<>();
////                //将解析出来的数据设置到physicValueFloat
////                for(int i = 0; i < TOTALPHYSICVAL; i++){
////                    bytes[i] = ByteBufUtil.getBytes(response.getRegisters(),i*4,4);
////                    //System.out.println("第"+(i+1)+"个"+"物理值: "+ByteBufUtil.hexDump(response.getRegisters(),i*4,4));
////                    physicValueInt[i] = CommonUtil.bytesToInt2(bytes[i],0);
////                    //System.out.println("第"+(i+1)+"个"+"物理值(转成十进制): "+ physicValue[i]);
////                    physicValueFloat[i] = (float)physicValueInt[i]/(float) 10000;
////                    System.out.println("第"+(i+1)+"个"+"物理值(转成十进制，除以10000): "+ physicValueFloat[i]);
////                    //map.put("第"+(i+1)+"个"+"物理值(转成十进制，除以10000): ", physicValueFloat[i]);
////                }
////
////                setValueAndTag(physicValueFloat,fbgValues);
////                //存储到数据库
////                //fbgValueMapper.multipleInsert(fbgValues);
////                //发送数据给mqtt服务器
////                try {
////                    //这此runner发送给mqtt服务器的主题为应变值，其他runner就改变主题即可区分是什么传感器发来的消息
////                    Publish.publish(fbgValues.toString(),mqttClient,"/tcp/strain");
////                } catch (MqttException e) {
////                    e.printStackTrace();
////                }
////                ReferenceCountUtil.release(response);
////            } else {
////                logger.error("Completed exceptionally, message={}", ex.getMessage(), ex);
////            }
////        }, scheduler);
////    }
//}