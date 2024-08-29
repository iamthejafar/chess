package com.gbkl.Chess.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gbkl.Chess.model.*;
import com.gbkl.Chess.service.GameSessionManager;
import com.gbkl.Chess.service.MatchmakingService;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;

import java.util.Optional;

@Controller
public class WebSocketController {

    private final MatchmakingService matchmakingService;
    private final GameSessionManager gameSessionManager;



    @Autowired
    public WebSocketController(MatchmakingService matchmakingService, GameSessionManager gameSessionManager) {
        this.matchmakingService = matchmakingService;
        this.gameSessionManager = gameSessionManager;
    }

    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payloadText = message.getPayload();
        System.out.println("Payload:"+payloadText);
        ObjectMapper objectMapper = new ObjectMapper();
        BasePayload basePayload = objectMapper.readValue(payloadText, BasePayload.class);
        switch (basePayload.getMessage()) {
            case "JOIN":
                JoinPayload joinPayload = objectMapper.readValue(payloadText, JoinPayload.class);
                GameSession gameSession = matchmakingService.pairPlayer(joinPayload);
                if (gameSession != null) {
                    session.sendMessage(new TextMessage(gameSession.toString()));
//                    session.sendMessage(new TextMessage("Game started with players " + gameSession.getPlayer1().getName() + " and " + gameSession.getPlayer2().getName()));

                } else {
                    session.sendMessage(new TextMessage("Waiting for an opponent..."));
                }
                break;
            case "MOVE":
                MovePayload movePayload = objectMapper.readValue(payloadText, MovePayload.class);
                Optional<GameSession> moveGameSession = gameSessionManager.getGameSession(movePayload.getGameId());

                if (moveGameSession.isEmpty()) {
                    session.sendMessage(new TextMessage("Game not found"));
                    break;
                }

                Move move = new Move(Square.fromValue(movePayload.getFrom()),Square.fromValue(movePayload.getTo()));
                if(moveGameSession.get().getBoard().doMove(move)){
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(moveGameSession.get())));
                }


                break;
            default:
                // Handle unknown message types
                break;
        }
    }

}
