package com.gbkl.Chess.service;
import com.gbkl.Chess.model.GameSession;
import com.gbkl.Chess.model.Player;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
public class GameSessionManager {


    ArrayList<GameSession> gameSessions = new ArrayList<>();

    public GameSession createNewGameSession(Player whitePlayer, Player blackPlayer){
        GameSession newGameSession = new GameSession(whitePlayer,blackPlayer);
        gameSessions.add(newGameSession);
        System.out.println(gameSessions.size());
        return newGameSession;
    }

    public Optional<GameSession> getGameSession(Player player){
        for (GameSession gameSession : gameSessions) {
            if(gameSession.getWhitePlayer().getUid().equals(player.getUid()) || gameSession.getBlackplayer().getUid().equals(player.getUid())){
                return Optional.of(gameSession);
            }
        }
        return Optional.empty();
    }

    public Optional<GameSession> getGameSession(String gameId){
        System.out.println(gameSessions.size());
        for (GameSession gameSession : gameSessions) {
            System.out.println(gameSession.toString());
            if(gameSession.getGameId().equals(gameId)){
                return Optional.of(gameSession);
            }
        }
        return Optional.empty();
    }
}
