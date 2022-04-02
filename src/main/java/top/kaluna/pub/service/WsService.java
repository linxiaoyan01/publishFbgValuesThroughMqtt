package top.kaluna.pub.service;

import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import top.kaluna.pub.websocket.WebSocketServer;

import javax.annotation.Resource;

/**
 * @author Yuery
 * @date 2022/2/28/0028 - 13:53
 */
@Service
public class WsService {

    @Resource
    private WebSocketServer webSocketServer;

    @Async
    public void sendInfo(String message, String logId){
        //往所有的session推送消息
        MDC.put("LOG_ID",logId);
        webSocketServer.sendInfo(message);
    }
}