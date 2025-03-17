package com.gbkl.Chess.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gbkl.Chess.model.*;
import com.gbkl.Chess.service.GameSessionManager;
import com.gbkl.Chess.service.MatchmakingService;
import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

@Controller
public class WebSocketController {
    private final MatchmakingService matchmakingService;
    private final GameSessionManager gameSessionManager;
    private final ObjectMapper objectMapper;

    @Autowired
    public WebSocketController(MatchmakingService matchmakingService, GameSessionManager gameSessionManager) {
        this.matchmakingService = matchmakingService;
        this.gameSessionManager = gameSessionManager;
        this.objectMapper = new ObjectMapper();
    }

    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payloadText = message.getPayload();
        BasePayload basePayload = objectMapper.readValue(payloadText, BasePayload.class);
        
        switch (basePayload.getMessage()) {
            case "JOIN":
                handleJoinRequest(session, payloadText);
                break;
            case "MOVE":
                handleMoveRequest(session, payloadText);
                break;
            case "RESIGN":
                handleResignRequest(session, payloadText);
                break;
            case "DRAW_OFFER":
                handleDrawOffer(session, payloadText);
                break;
            case "DRAW_RESPONSE":
                handleDrawResponse(session, payloadText);
                break;
            default:
                session.sendMessage(new TextMessage(createErrorResponse("Unknown message type")));
                break;
        }
    }

    private void handleJoinRequest(WebSocketSession session, String payloadText) throws Exception {
        JoinPayload joinPayload = objectMapper.readValue(payloadText, JoinPayload.class);
        GameSession gameSession = matchmakingService.pairPlayer(joinPayload);
        
        if (gameSession != null) {
            GameStateResponse response = new GameStateResponse(
                gameSession.getGameId(),
                gameSession.getBoard().getFen(),
                gameSession.getWhitePlayer(),
                gameSession.getBlackplayer(),
                gameSession.getBoard().getSideToMove() == Side.WHITE ? "WHITE" : "BLACK",
                gameSession.getBoard().isKingAttacked(),
                false
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } else {
            session.sendMessage(new TextMessage(createStatusResponse("Waiting for an opponent...")));
        }
    }

    private void handleMoveRequest(WebSocketSession session, String payloadText) throws Exception {
        MovePayload movePayload = objectMapper.readValue(payloadText, MovePayload.class);
        Optional<GameSession> optGameSession = gameSessionManager.getGameSession(movePayload.getGameId());

        if (optGameSession.isEmpty()) {
            session.sendMessage(new TextMessage(createErrorResponse("Game not found")));
            return;
        }

        GameSession gameSession = optGameSession.get();
        Board board = gameSession.getBoard();
        
        // Validate player's turn
        String playerColor = movePayload.getPlayerUid().equals(gameSession.getWhitePlayer().getUid()) ? "WHITE" : "BLACK";
        if ((board.getSideToMove() == Side.WHITE && !playerColor.equals("WHITE")) ||
            (board.getSideToMove() == Side.BLACK && !playerColor.equals("BLACK"))) {
            session.sendMessage(new TextMessage(createErrorResponse("Not your turn")));
            return;
        }

        try {
            Move move = new Move(
                Square.fromValue(movePayload.getFrom()),
                Square.fromValue(movePayload.getTo()),
                movePayload.getPromotionPiece() != null ? Piece.valueOf(movePayload.getPromotionPiece()) : null
            );

            // Validate move
            if (!board.legalMoves().contains(move)) {
                session.sendMessage(new TextMessage(createErrorResponse("Illegal move")));
                return;
            }

            // Make the move
            board.doMove(move);

            // Check game state
            boolean isCheck = board.isKingAttacked();
            boolean isCheckmate = board.isMated();
            boolean isStalemate = board.isStaleMate();
            boolean isDrawByRepetition = board.isRepetition();
            boolean isDrawByInsufficientMaterial = board.isInsufficientMaterial();

            GameStateResponse response = new GameStateResponse(
                gameSession.getGameId(),
                board.getFen(),
                gameSession.getWhitePlayer(),
                gameSession.getBlackplayer(),
                board.getSideToMove() == Side.WHITE ? "WHITE" : "BLACK",
                isCheck,
                isCheckmate
            );

            if (isCheckmate) {
                response.setGameOver(true);
                response.setWinner(playerColor);
                response.setEndReason("checkmate");
            } else if (isStalemate) {
                response.setGameOver(true);
                response.setEndReason("stalemate");
            } else if (isDrawByRepetition) {
                response.setGameOver(true);
                response.setEndReason("repetition");
            } else if (isDrawByInsufficientMaterial) {
                response.setGameOver(true);
                response.setEndReason("insufficient material");
            }

            gameSessionManager.broadcastToPlayers(gameSession, objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            session.sendMessage(new TextMessage(createErrorResponse("Invalid move: " + e.getMessage())));
        }
    }

    private void handleResignRequest(WebSocketSession session, String payloadText) throws Exception {
        ResignPayload resignPayload = objectMapper.readValue(payloadText, ResignPayload.class);
        Optional<GameSession> optGameSession = gameSessionManager.getGameSession(resignPayload.getGameId());

        if (optGameSession.isEmpty()) {
            session.sendMessage(new TextMessage(createErrorResponse("Game not found")));
            return;
        }

        GameSession gameSession = optGameSession.get();
        String winner = resignPayload.getPlayerUid().equals(gameSession.getWhitePlayer().getUid()) ? "BLACK" : "WHITE";

        GameStateResponse response = new GameStateResponse(
            gameSession.getGameId(),
            gameSession.getBoard().getFen(),
            gameSession.getWhitePlayer(),
            gameSession.getBlackplayer(),
            null,
            false,
            true
        );
        response.setGameOver(true);
        response.setWinner(winner);
        response.setEndReason("resignation");

        gameSessionManager.broadcastToPlayers(gameSession, objectMapper.writeValueAsString(response));
    }

    private void handleDrawOffer(WebSocketSession session, String payloadText) throws Exception {
        DrawOfferPayload drawPayload = objectMapper.readValue(payloadText, DrawOfferPayload.class);
        Optional<GameSession> optGameSession = gameSessionManager.getGameSession(drawPayload.getGameId());

        if (optGameSession.isEmpty()) {
            session.sendMessage(new TextMessage(createErrorResponse("Game not found")));
            return;
        }

        GameSession gameSession = optGameSession.get();
        gameSession.setDrawOfferedBy(drawPayload.getPlayerUid());
        
        GameStateResponse response = new GameStateResponse(
            gameSession.getGameId(),
            gameSession.getBoard().getFen(),
            gameSession.getWhitePlayer(),
            gameSession.getBlackplayer(),
            gameSession.getBoard().getSideToMove() == Side.WHITE ? "WHITE" : "BLACK",
            gameSession.getBoard().isKingAttacked(),
            false
        );
        response.setDrawOffered(true);
        response.setDrawOfferedBy(drawPayload.getPlayerUid());

        gameSessionManager.broadcastToPlayers(gameSession, objectMapper.writeValueAsString(response));
    }

    private void handleDrawResponse(WebSocketSession session, String payloadText) throws Exception {
        DrawResponsePayload drawResponse = objectMapper.readValue(payloadText, DrawResponsePayload.class);
        Optional<GameSession> optGameSession = gameSessionManager.getGameSession(drawResponse.getGameId());

        if (optGameSession.isEmpty()) {
            session.sendMessage(new TextMessage(createErrorResponse("Game not found")));
            return;
        }

        GameSession gameSession = optGameSession.get();
        
        if (drawResponse.isAccepted()) {
            GameStateResponse response = new GameStateResponse(
                gameSession.getGameId(),
                gameSession.getBoard().getFen(),
                gameSession.getWhitePlayer(),
                gameSession.getBlackplayer(),
                null,
                false,
                true
            );
            response.setGameOver(true);
            response.setEndReason("draw by agreement");
            gameSessionManager.broadcastToPlayers(gameSession, objectMapper.writeValueAsString(response));
        } else {
            gameSession.setDrawOfferedBy(null);
            GameStateResponse response = new GameStateResponse(
                gameSession.getGameId(),
                gameSession.getBoard().getFen(),
                gameSession.getWhitePlayer(),
                gameSession.getBlackplayer(),
                gameSession.getBoard().getSideToMove() == Side.WHITE ? "WHITE" : "BLACK",
                gameSession.getBoard().isKingAttacked(),
                false
            );
            response.setDrawOffered(false);
            gameSessionManager.broadcastToPlayers(gameSession, objectMapper.writeValueAsString(response));
        }
    }

    private String createErrorResponse(String message) throws Exception {
        ErrorResponse error = new ErrorResponse(message);
        return objectMapper.writeValueAsString(error);
    }

    private String createStatusResponse(String message) throws Exception {
        StatusResponse status = new StatusResponse(message);
        return objectMapper.writeValueAsString(status);
    }
}
