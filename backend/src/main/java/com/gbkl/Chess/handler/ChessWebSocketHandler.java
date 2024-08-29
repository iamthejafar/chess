package com.gbkl.Chess.handler;

import com.gbkl.Chess.controller.WebSocketController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.*;

@Configuration
public class ChessWebSocketHandler implements WebSocketHandler {

    final WebSocketController webSocketController;
    ChessWebSocketHandler(WebSocketController webSocketController){
        this.webSocketController = webSocketController;
    }
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.sendMessage(new TextMessage("Connection Established."));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        System.out.println(session);
        System.out.println(message);
        System.out.println("From Handler");
        webSocketController.handleTextMessage(session, (TextMessage) message);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.out.println("handleTransportError");
        System.out.println(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        System.out.println("afterConnectionClosed");
        System.out.println(session);
        System.out.println(closeStatus);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
