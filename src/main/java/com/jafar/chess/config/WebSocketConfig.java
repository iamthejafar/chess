package com.jafar.chess.config;


import com.jafar.chess.controller.ChessWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

import java.util.Arrays;


@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChessWebSocketHandler chessWebSocketHandler;
    private final String allowedOrigins;

    public WebSocketConfig(
            ChessWebSocketHandler chessWebSocketHandler,
            @Value("${app.cors.allowed-origins:http://localhost:3000}") String allowedOrigins) {
        this.chessWebSocketHandler = chessWebSocketHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] resolvedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .map(this::stripQuotes)
                .map(this::removeTrailingSlash)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);

        registry.addHandler(chessWebSocketHandler, "/chess")
                .setAllowedOriginPatterns(resolvedOrigins);
    }

    private String stripQuotes(String origin) {
        if ((origin.startsWith("\"") && origin.endsWith("\""))
                || (origin.startsWith("'") && origin.endsWith("'"))) {
            return origin.substring(1, origin.length() - 1).trim();
        }
        return origin;
    }

    private String removeTrailingSlash(String origin) {
        return origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
    }
}
