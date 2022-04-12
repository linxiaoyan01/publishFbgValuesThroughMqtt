//package top.kaluna.pub.job;
//
//import com.alibaba.fastjson.JSONObject;
//import org.eclipse.paho.client.mqttv3.MqttClient;
//import org.eclipse.paho.client.mqttv3.MqttException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.slf4j.MDC;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import top.kaluna.pub.domain.FbgValue;
//import top.kaluna.pub.runner.Publish;
//import top.kaluna.pub.util.DateUtil;
//import top.kaluna.pub.util.RandomUtil;
//import top.kaluna.pub.util.SnowFlake;
//import org.springframework.transaction.annotation.Transactional;
//import javax.annotation.Resource;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * @author Yuery
// * @date 2022/2/28/0028 - 11:11
// * 定时任务
// */
//@Component
//public class PubJob {
//    private final Logger LOG = LoggerFactory.getLogger(PubJob.class);
//    @Resource
//    private SnowFlake snowFlake;
//
//
//    String userName = "tumour";
//    String passWord = "tumour";
//    MqttClient mqttClient = Publish.connect(userName, passWord);
//
//    public PubJob() throws MqttException {
//
//    }
//
//    @Transactional(rollbackFor = Exception.class)
//    @Scheduled(cron = "0/5 * * * * ? " )
//    public void cronSendToVibration() throws MqttException {
//        List<FbgValue> vibrationfbgValues = new ArrayList<>();
//        List<FbgValue> strainfbgValues = new ArrayList<>();
//        //创建五个振动值 和 二十个应变值 分不同主题发送给订阅者? 还是不要分直接一起发，转成JsonString?
//        //采集如何区分？
//        for(int i = 0; i < 5; i++){
//            vibrationfbgValues.add(new FbgValue(new Long(Long.toString(i)), new Long(Long.toString(i+22)), RandomUtil.From100TO1000(), (byte) 0, DateUtil.getNowTime().getTime()));
//        }
//        for(int i = 0; i < 20; i++){
//            strainfbgValues.add(new FbgValue(new Long(Long.toString(i)), new Long(Long.toString(i+2)), RandomUtil.From100TO1000(), (byte) 0, DateUtil.getNowTime().getTime()));
//        }
//        List<List<FbgValue>> list = new ArrayList<>();
//        list.add(vibrationfbgValues);
//        list.add(strainfbgValues);
//        String astr = JSONObject.toJSONString(list);
//        //增加日志流水号
//        MDC.put("LOG_ID",String.valueOf(snowFlake.nextId()));
//        String logId = MDC.get("LOG_ID");
//        LOG.info("推送新消息");
//        Publish.publish(astr, mqttClient,"/tcp/fbg");
//
//        //Publish.publish(strainfbgValues, mqttClient,"/tcp/strain");
//        long start = System.currentTimeMillis();
//        LOG.info("推送新消息结束，耗时：{}毫秒",System.currentTimeMillis() - start);
//    }
//}
//
