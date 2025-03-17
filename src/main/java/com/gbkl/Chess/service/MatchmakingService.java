package com.gbkl.Chess.service;


import com.gbkl.Chess.model.GameSession;
import com.gbkl.Chess.model.JoinPayload;
import com.gbkl.Chess.model.Player;
import lombok.Synchronized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MatchmakingService {

    private Player waitingPlayer = null;

    @Autowired
    private GameSessionManager gameSessionManager;

    @Synchronized
    public GameSession pairPlayer(JoinPayload joinPayload) {
        if (waitingPlayer == null) {
            waitingPlayer = Player.builder()
                    .uid(joinPayload.getUid())
                    .Color("WHITE")
                    .name(joinPayload.getName())
                    .build();
            return null;
        } else {
            Player opponent = waitingPlayer;
            waitingPlayer = null;
            Player newPlayer = Player.builder()
                    .uid(joinPayload.getUid())
                    .Color("BLACK")
                    .name(joinPayload.getName())
                    .build();
            return gameSessionManager.createNewGameSession(opponent,newPlayer);
        }
    }

}
