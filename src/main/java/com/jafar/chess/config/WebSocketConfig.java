package com.jafar.chess.config;


import com.jafar.chess.controller.ChessWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;


@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChessWebSocketHandler chessWebSocketHandler;
    private final String[] allowedOrigins;

    public WebSocketConfig(
            ChessWebSocketHandler chessWebSocketHandler,
            @Value("${app.cors.allowed-origins:http://localhost:3000}") String[] allowedOrigins) {
        this.chessWebSocketHandler = chessWebSocketHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chessWebSocketHandler, "/chess")
                .setAllowedOrigins(allowedOrigins);
    }
}
