package com.jafar.chess.controller;


import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.MoveConversionException;
import com.jafar.chess.dto.request.GameActionRequest;
import com.jafar.chess.dto.request.InitGameRequest;
import com.jafar.chess.dto.request.MoveRequest;
import com.jafar.chess.dto.request.WsRequest;
import com.jafar.chess.dto.response.GameResultResponse;
import com.jafar.chess.dto.response.GameStateResponse;
import com.jafar.chess.dto.response.GameStatusResponse;
import com.jafar.chess.model.Game;
import com.jafar.chess.model.MatchResult;
import com.jafar.chess.service.GameService;
import com.jafar.chess.shared.EndReason;
import com.jafar.chess.shared.GameResult;
import com.jafar.chess.shared.Messages;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChessWebSocketHandler extends TextWebSocketHandler {

    private final GameService gameService;
    private final ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessionsByUserId = new ConcurrentHashMap<>();
    private final Map<String, String> userIdBySessionId = new ConcurrentHashMap<>();

    public ChessWebSocketHandler(GameService gameService, ObjectMapper objectMapper) {
        this.gameService = gameService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Player connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        System.out.println("Handle Message");
        System.out.println(payload);

        WsRequest request;
        try {
            request = objectMapper.readValue(payload, WsRequest.class);
        } catch (Exception e) {
            sendError(session, "Invalid message format");
            return;
        }

        if (request.getType() == null) {
            sendError(session, "Missing message type");
            return;
        }

        switch (request.getType()) {
            case INIT_GAME:
                handleInitGame(session, objectMapper.readValue(payload, InitGameRequest.class));
                break;
            case MOVE:
                handleMove(session, objectMapper.readValue(payload, MoveRequest.class));
                break;
            case RESIGN:
                handleResign(session, objectMapper.readValue(payload, GameActionRequest.class));
                break;
            case DRAW_OFFER:
                handleDrawOffer(session, objectMapper.readValue(payload, GameActionRequest.class));
                break;
            case DRAW_ACCEPT:
                handleDrawAccept(session, objectMapper.readValue(payload, GameActionRequest.class));
                break;
            case DRAW_DECLINE:
                handleDrawDecline(session, objectMapper.readValue(payload, GameActionRequest.class));
                break;
            default:
                sendError(session, "Unsupported message type: " + request.getType());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = userIdBySessionId.remove(session.getId());
        if (userId != null) {
            sessionsByUserId.remove(userId);
            gameService.removeUser(userId);
        }
        System.out.println("Connection closed: " + session.getId());
    }

    private void handleInitGame(WebSocketSession session, InitGameRequest request) throws IOException {
        String userId = request.getUserId();
        if (userId == null || userId.isBlank()) {
            sendError(session, "userId is required");
            return;
        }

        bindSessionToUser(session, userId);

        MatchResult result;
        try {
            result = gameService.requestMatch(userId);
        } catch (IllegalArgumentException e) {
            sendError(session, e.getMessage());
            return;
        }

        if (result.getStatus() == MatchResult.Status.WAITING) {
            return;
        }

        Game game = gameService.getGame(result.getGameId());
        if (game == null) {
            sendError(session, "Game not found");
            return;
        }

        String userColor = resolveUserColor(game, userId);
        if (userColor == null) {
            sendError(session, "User color is not set for this game");
            return;
        }

        sendStatus(session, GameStatusResponse.builder()
                .type(Messages.MATCHED)
                .message(userColor)
                .userId(userId)
                .opponentUserId(result.getOpponentUserId())
                .gameId(result.getGameId())
                .sessionId(session.getId())
                .build());

        WebSocketSession opponentSession = sessionsByUserId.get(result.getOpponentUserId());
        if (opponentSession != null && opponentSession.isOpen()) {
            String opponentColor = resolveUserColor(game, result.getOpponentUserId());
            if (opponentColor == null) {
                sendError(opponentSession, "User color is not set for this game");
                return;
            }

            sendStatus(opponentSession, GameStatusResponse.builder()
                    .type(Messages.MATCHED)
                    .message(opponentColor)
                    .userId(result.getOpponentUserId())
                    .opponentUserId(userId)
                    .gameId(result.getGameId())
                    .sessionId(opponentSession.getId())
                    .build());
        }
    }

    private void handleMove(WebSocketSession session, MoveRequest request) throws IOException {
        String userId = request.getUserId();
        if (userId == null || userId.isBlank()) {
            userId = userIdBySessionId.get(session.getId());
        }
        if (userId == null || userId.isBlank()) {
            sendError(session, "userId is required for MOVE");
            return;
        }

        String gameId = request.getGameId();
        if (gameId == null || gameId.isBlank()) {
            gameId = gameService.findGameId(userId);
        }
        if (gameId == null || gameId.isBlank()) {
            sendError(session, "gameId is required for MOVE");
            return;
        }

        if (request.getFrom() == null || request.getFrom().isBlank()
                || request.getTo() == null || request.getTo().isBlank()) {
            sendError(session, "from and to are required for MOVE");
            return;
        }

        Game game = gameService.getGame(gameId);
        Board board = gameService.getBoard(gameId);
        if (game == null) {
            sendError(session, "Game not found");
            return;
        }
        if (board == null) {
            sendError(session, "Game board not found");
            return;
        }
        if (!game.isParticipant(userId)) {
            sendError(session, "User is not part of this game");
            return;
        }
        if (game.isGameOver()) {
            sendError(session, "Game is already over");
            return;
        }

        Side userSide = resolveUserSide(game, userId);
        if (userSide == null) {
            sendError(session, "User color is not set for this game");
            return;
        }

        if (board.getSideToMove() != userSide) {
            sendError(session, "Not your turn");
            return;
        }

        String moveValue;
        try {
            moveValue = buildMoveValue(request);
        } catch (IllegalArgumentException e) {
            sendError(session, e.getMessage());
            return;
        }

        boolean applied;
        try {
            applied = board.doMove(moveValue);
        } catch (MoveConversionException | IllegalArgumentException e) {
            sendError(session, "Illegal move");
            return;
        }

        if (!applied) {
            sendError(session, "Illegal move");
            return;
        }

        game.addMove(moveValue);
        gameService.clearDrawOffer(gameId);
        updateOutcomeFromBoard(game, board);
        gameService.stageLiveUpdate(game, board);
        gameService.applyRatingsIfGameOver(game);

        sendGameStateToPlayers(game, board, Messages.MOVE, moveValue);
    }

    private void handleResign(WebSocketSession session, GameActionRequest request) throws IOException {
        ActionContext context = resolveActionContext(session, request, "RESIGN");
        if (context == null) {
            return;
        }

        Game game;
        try {
            game = gameService.resign(context.userId, context.gameId);
        } catch (IllegalArgumentException e) {
            sendError(session, e.getMessage());
            return;
        }

        sendGameStateToPlayers(game, context.board, Messages.RESIGN, null);
    }

    private void handleDrawOffer(WebSocketSession session, GameActionRequest request) throws IOException {
        ActionContext context = resolveActionContext(session, request, "DRAW_OFFER");
        if (context == null) {
            return;
        }

        Game game;
        try {
            game = gameService.offerDraw(context.userId, context.gameId);
        } catch (IllegalArgumentException e) {
            sendError(session, e.getMessage());
            return;
        }

        sendGameStateToPlayers(game, context.board, Messages.DRAW_OFFER, null);
    }

    private void handleDrawAccept(WebSocketSession session, GameActionRequest request) throws IOException {
        ActionContext context = resolveActionContext(session, request, "DRAW_ACCEPT");
        if (context == null) {
            return;
        }

        Game game;
        try {
            game = gameService.acceptDraw(context.userId, context.gameId);
        } catch (IllegalArgumentException e) {
            sendError(session, e.getMessage());
            return;
        }

        sendGameStateToPlayers(game, context.board, Messages.DRAW_ACCEPT, null);
    }

    private void handleDrawDecline(WebSocketSession session, GameActionRequest request) throws IOException {
        ActionContext context = resolveActionContext(session, request, "DRAW_DECLINE");
        if (context == null) {
            return;
        }

        Game game;
        try {
            game = gameService.declineDraw(context.userId, context.gameId);
        } catch (IllegalArgumentException e) {
            sendError(session, e.getMessage());
            return;
        }

        sendGameStateToPlayers(game, context.board, Messages.DRAW_DECLINE, null);
    }

    private ActionContext resolveActionContext(WebSocketSession session, GameActionRequest request, String action)
            throws IOException {
        String userId = request.getUserId();
        if (userId == null || userId.isBlank()) {
            userId = userIdBySessionId.get(session.getId());
        }
        if (userId == null || userId.isBlank()) {
            sendError(session, "userId is required for " + action);
            return null;
        }

        String gameId = request.getGameId();
        if (gameId == null || gameId.isBlank()) {
            gameId = gameService.findGameId(userId);
        }
        if (gameId == null || gameId.isBlank()) {
            sendError(session, "gameId is required for " + action);
            return null;
        }

        Board board = gameService.getBoard(gameId);
        if (board == null) {
            sendError(session, "Game board not found");
            return null;
        }

        return new ActionContext(userId, gameId, board);
    }

    private void bindSessionToUser(WebSocketSession session, String userId) {
        sessionsByUserId.put(userId, session);
        userIdBySessionId.put(session.getId(), userId);
    }

    private void sendStatus(WebSocketSession session, GameStatusResponse response) throws IOException {
        sendJson(session, response);
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        sendStatus(session, GameStatusResponse.builder()
                .type(Messages.ERROR)
                .message(message)
                .userId(userIdBySessionId.get(session.getId()))
                .sessionId(session.getId())
                .build());
    }

    private void sendGameStateToPlayers(Game game, Board board, Messages type, String lastMove) throws IOException {
        GameStateResponse response = GameStateResponse.builder()
                .type(type)
                .gameId(game.getGameId())
                .whiteUserId(game.getWhiteUserId())
                .blackUserId(game.getBlackUserId())
                .moveCount(game.getMoveCount())
                .fen(board.getFen())
                .lastMove(lastMove)
                .moves(game.getMoves())
                .gameOver(game.isGameOver())
                .gameResult(resolveGameResult(game))
                .drawOffered(game.isDrawOffered())
                .drawOfferedByUserId(game.getDrawOfferedByUserId())
                .build();

        sendJson(sessionsByUserId.get(game.getWhiteUserId()), response);
        sendJson(sessionsByUserId.get(game.getBlackUserId()), response);
    }

    private GameResultResponse resolveGameResult(Game game) {
        if (game == null || !game.isGameOver()) {
            return null;
        }

        return GameResultResponse.builder()
                .result(game.getResult())
                .endReason(game.getEndReason())
                .winnerUserId(game.getWinnerUserId())
                .build();
    }

    private void updateOutcomeFromBoard(Game game, Board board) {
        if (game.isGameOver()) {
            return;
        }
        if (board.isMated()) {
            game.conclude(
                    GameResult.CHECKMATE,
                    EndReason.CHECKMATE,
                    board.getSideToMove() == Side.WHITE ? game.getBlackUserId() : game.getWhiteUserId());
            return;
        }
        if (isRepetitionDraw(board)) {
            game.conclude(GameResult.DRAW, EndReason.THREEFOLD_REPETITION, null);
            return;
        }
        if (board.isStaleMate()) {
            game.conclude(GameResult.DRAW, EndReason.STALEMATE, null);
            return;
        }
        if (board.isInsufficientMaterial()) {
            game.conclude(GameResult.DRAW, EndReason.INSUFFICIENT_MATERIAL, null);
            return;
        }
        if (board.isDraw()) {
            game.conclude(GameResult.DRAW, EndReason.MUTUAL_AGREEMENT, null);
            return;
        }

        game.setResult(GameResult.IN_PROGRESS);
        game.setEndReason(null);
        game.setWinnerUserId(null);
    }

    private boolean isRepetitionDraw(Board board) {
        // chesslib versions expose repetition with slightly different method names.
        return invokeBoolean(board, "isRepetition") || invokeBoolean(board, "isThreefoldRepetition");
    }

    private boolean invokeBoolean(Board board, String methodName) {
        try {
            Method method = board.getClass().getMethod(methodName);
            Object value = method.invoke(board);
            return value instanceof Boolean && (Boolean) value;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private Side resolveUserSide(Game game, String userId) {
        if (userId == null) {
            return null;
        }
        if (userId.equals(game.getWhiteUserId())) {
            return Side.WHITE;
        }
        if (userId.equals(game.getBlackUserId())) {
            return Side.BLACK;
        }
        return null;
    }

    private String resolveUserColor(Game game, String userId) {
        Side side = resolveUserSide(game, userId);
        return side == null ? null : side.name();
    }

    private String buildMoveValue(MoveRequest request) {
        String promotionSuffix = normalizePromotion(request.getPromotion());
        return request.getFrom().toLowerCase(Locale.ROOT)
                + request.getTo().toLowerCase(Locale.ROOT)
                + promotionSuffix;
    }

    private String normalizePromotion(String promotion) {
        if (promotion == null || promotion.isBlank()) {
            return "";
        }

        return switch (promotion.trim().toLowerCase(Locale.ROOT)) {
            case "q", "queen" -> "q";
            case "r", "rook" -> "r";
            case "b", "bishop" -> "b";
            case "n", "knight" -> "n";
            default -> throw new IllegalArgumentException("promotion must be one of: q, r, b, n");
        };
    }

    private void sendJson(WebSocketSession session, Object payload) throws IOException {
        if (session == null || !session.isOpen()) {
            return;
        }
        String json = objectMapper.writeValueAsString(payload);
        System.out.println("Send Message");
        System.out.println(json);
        session.sendMessage(new TextMessage(json));
    }

    private static class ActionContext {
        private final String userId;
        private final String gameId;
        private final Board board;

        private ActionContext(String userId, String gameId, Board board) {
            this.userId = userId;
            this.gameId = gameId;
            this.board = board;
        }
    }
}
