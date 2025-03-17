package com.gbkl.Chess.service;

import com.gbkl.Chess.model.GameSession;
import com.gbkl.Chess.model.Player;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameSessionManager {
    private final List<GameSession> gameSessions = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, WebSocketSession> playerSessions = new ConcurrentHashMap<>();

    public GameSession createNewGameSession(Player whitePlayer, Player blackPlayer) {
        GameSession newGameSession = new GameSession(whitePlayer, blackPlayer);
        gameSessions.add(newGameSession);
        return newGameSession;
    }

    public void registerPlayerSession(String playerId, WebSocketSession session) {
        playerSessions.put(playerId, session);
    }

    public void removePlayerSession(String playerId) {
        playerSessions.remove(playerId);
    }

    public Optional<GameSession> getGameSession(Player player) {
        synchronized(gameSessions) {
            return gameSessions.stream()
                .filter(gs -> gs.getWhitePlayer().getUid().equals(player.getUid()) ||
                            gs.getBlackplayer().getUid().equals(player.getUid()))
                .findFirst();
        }
    }

    public Optional<GameSession> getGameSession(String gameId) {
        synchronized(gameSessions) {
            return gameSessions.stream()
                .filter(gs -> gs.getGameId().equals(gameId))
                .findFirst();
        }
    }

    public void broadcastToPlayers(GameSession gameSession, String message) throws IOException {
        WebSocketSession whiteSession = playerSessions.get(gameSession.getWhitePlayer().getUid());
        WebSocketSession blackSession = playerSessions.get(gameSession.getBlackplayer().getUid());
        TextMessage textMessage = new TextMessage(message);

        if (whiteSession != null && whiteSession.isOpen()) {
            whiteSession.sendMessage(textMessage);
        }
        if (blackSession != null && blackSession.isOpen()) {
            blackSession.sendMessage(textMessage);
        }

        if (gameSession.isGameOver()) {
            endGame(gameSession);
        }
    }

    private void endGame(GameSession gameSession) {
        gameSessions.remove(gameSession);
    }
}
