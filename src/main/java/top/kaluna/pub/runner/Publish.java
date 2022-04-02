package top.kaluna.pub.runner;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import top.kaluna.pub.domain.FbgValue;

import java.util.List;

/**
 * @author Yuery
 * @date 2022/4/1/0001 - 22:09
 * 发送数据到mqtt服务器
 */
public class Publish {
    public static MqttClient connect(String userName,
                                      String password) throws MqttException {
        MemoryPersistence persistence = new MemoryPersistence();
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setUserName(userName);
        connOpts.setPassword(password.toCharArray());
        connOpts.setConnectionTimeout(10);
        connOpts.setKeepAliveInterval(20);
//      String[] uris = {"tcp://10.100.124.206:1883","tcp://10.100.124.207:1883"};
//      connOpts.setServerURIs(uris);  //起到负载均衡和高可用的作用
        String broker = "tcp://101.132.252.118:1883";
        MqttClient mqttClient = new MqttClient(broker, "client-id-0", persistence);
        mqttClient.setCallback(new PushCallback("test0"));
        mqttClient.connect(connOpts);
        return mqttClient;
    }

    public static void publish(String fbgValues, MqttClient sampleClient, String topic)
            throws MqttException {
        MqttMessage message = new MqttMessage(fbgValues.getBytes());
        int qos = 2;
        message.setQos(qos);
        message.setRetained(false);
        sampleClient.publish(topic, message);
    }
}

class PushCallback implements MqttCallback {
    public PushCallback(String threadId){
    }
    @Override
    public void connectionLost(Throwable cause) {

    }
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("deliveryComplete---------" + token.isComplete());
    }
    @Override
    public void messageArrived(String topic, MqttMessage message) {
    }
}
