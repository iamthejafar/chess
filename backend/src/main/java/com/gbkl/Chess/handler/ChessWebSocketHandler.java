package com.gbkl.Chess.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gbkl.Chess.controller.WebSocketController;
import com.gbkl.Chess.model.BasePayload;
import com.gbkl.Chess.model.ErrorResponse;
import com.gbkl.Chess.model.JoinPayload;
import com.gbkl.Chess.service.GameSessionManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.*;

@Configuration
public class ChessWebSocketHandler implements WebSocketHandler {
    private final WebSocketController webSocketController;
    private final GameSessionManager gameSessionManager;
    private final ObjectMapper objectMapper;

    public ChessWebSocketHandler(WebSocketController webSocketController, GameSessionManager gameSessionManager) {
        this.webSocketController = webSocketController;
        this.gameSessionManager = gameSessionManager;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // The actual player registration will happen when they send their first message with their UID
        session.sendMessage(new TextMessage("Connection established"));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            String payload = textMessage.getPayload();
            
            try {
                // First try to parse as BasePayload to get the message type
                BasePayload basePayload = objectMapper.readValue(payload, BasePayload.class);
                
                if ("JOIN".equals(basePayload.getMessage())) {
                    // Parse again as JoinPayload to validate the full structure
                    JoinPayload joinPayload = objectMapper.readValue(payload, JoinPayload.class);
                    // Use the UID from the payload for session management
                    if (joinPayload.getUid() != null) {
                        gameSessionManager.registerPlayerSession(joinPayload.getUid(), session);
                    }
                }
                
                // Forward the message to the controller
                webSocketController.handleTextMessage(session, textMessage);
            } catch (Exception e) {
                e.printStackTrace(); // Log the error
                ErrorResponse error = new ErrorResponse("Invalid message format: " + e.getMessage());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String playerId = extractPlayerIdFromSession(session);
        if (playerId != null) {
            gameSessionManager.removePlayerSession(playerId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String playerId = extractPlayerIdFromSession(session);
        if (playerId != null) {
            gameSessionManager.removePlayerSession(playerId);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private String extractPlayerIdFromSession(WebSocketSession session) {
        // Extract player ID from session attributes or query parameters
        // This implementation depends on how you're passing the player ID in the connection
        return session.getAttributes().getOrDefault("playerId", null).toString();
    }
}
