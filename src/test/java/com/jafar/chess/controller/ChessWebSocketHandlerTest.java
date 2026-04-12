package com.jafar.chess.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import com.jafar.chess.dto.response.GameStateResponse;
import com.jafar.chess.dto.response.GameStatusResponse;
import com.jafar.chess.model.Game;
import com.jafar.chess.service.GameService;
import com.jafar.chess.shared.EndReason;
import com.jafar.chess.shared.GameResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChessWebSocketHandlerTest {

    private record MatchContext(
            String gameId,
            String whiteUserId,
            String blackUserId,
            WebSocketSession whiteSession,
            WebSocketSession blackSession,
            List<String> whiteMessages,
            List<String> blackMessages) {
    }

    @Test
    void playerReportedAsWhiteCanMakeTheFirstMove() throws Exception {
        GameService gameService = new GameService();
        ObjectMapper objectMapper = new ObjectMapper();
        ChessWebSocketHandler handler = new ChessWebSocketHandler(gameService, objectMapper);

        List<String> sessionOneMessages = new ArrayList<>();
        List<String> sessionTwoMessages = new ArrayList<>();
        WebSocketSession sessionOne = mockSession("session-1", sessionOneMessages);
        WebSocketSession sessionTwo = mockSession("session-2", sessionTwoMessages);

        String userOne = "4c4dcf22-4d4a-486c-b12f-a38648e4f501";
        String userTwo = "c4f8c954-0320-4853-94a7-7a40e6cd6ab9";

        MatchContext context = initializeMatch(
                handler,
                objectMapper,
                gameService,
                sessionOne,
                sessionTwo,
                sessionOneMessages,
                sessionTwoMessages,
                userOne,
                userTwo);

        assertEquals("WHITE", resolveColorMessage(context.whiteMessages.get(0), context.blackMessages.get(0), objectMapper));

        handler.handleTextMessage(context.whiteSession,
                new TextMessage("""
                        {"type":"MOVE","gameId":"%s","userId":"%s","from":"e2","to":"e4"}
                        """.formatted(context.gameId, context.whiteUserId).trim()));

        assertEquals(2, context.whiteMessages.size());
        assertEquals(2, context.blackMessages.size());

        GameStateResponse whiteMoveUpdate = objectMapper.readValue(context.whiteMessages.get(1), GameStateResponse.class);
        GameStateResponse blackMoveUpdate = objectMapper.readValue(context.blackMessages.get(1), GameStateResponse.class);

        assertEquals("MOVE", whiteMoveUpdate.getType().name());
        assertEquals("MOVE", blackMoveUpdate.getType().name());
        assertEquals("e2e4", whiteMoveUpdate.getLastMove());
        assertEquals("e2e4", blackMoveUpdate.getLastMove());
        assertFalse(whiteMoveUpdate.isGameOver());
        assertNull(whiteMoveUpdate.getGameResult());
    }

    @Test
    void checkmateAfterMoveMarksGameOverAndWinner() throws Exception {
        GameService gameService = new GameService();
        ObjectMapper objectMapper = new ObjectMapper();
        ChessWebSocketHandler handler = new ChessWebSocketHandler(gameService, objectMapper);

        List<String> sessionOneMessages = new ArrayList<>();
        List<String> sessionTwoMessages = new ArrayList<>();
        WebSocketSession sessionOne = mockSession("session-1", sessionOneMessages);
        WebSocketSession sessionTwo = mockSession("session-2", sessionTwoMessages);

        String userOne = "user-one";
        String userTwo = "user-two";

        MatchContext context = initializeMatch(
                handler,
                objectMapper,
                gameService,
                sessionOne,
                sessionTwo,
                sessionOneMessages,
                sessionTwoMessages,
                userOne,
                userTwo);

        playMove(handler, context.whiteSession, context.gameId, context.whiteUserId, "f2", "f3");
        playMove(handler, context.blackSession, context.gameId, context.blackUserId, "e7", "e5");
        playMove(handler, context.whiteSession, context.gameId, context.whiteUserId, "g2", "g4");
        playMove(handler, context.blackSession, context.gameId, context.blackUserId, "d8", "h4");

        GameStateResponse finalState = objectMapper.readValue(
                context.whiteMessages.get(context.whiteMessages.size() - 1),
                GameStateResponse.class);

        assertTrue(finalState.isGameOver());
        assertNotNull(finalState.getGameResult());
        assertEquals(GameResult.CHECKMATE, finalState.getGameResult().getResult());
        assertEquals(EndReason.CHECKMATE, finalState.getGameResult().getEndReason());
        assertEquals(context.blackUserId, finalState.getGameResult().getWinnerUserId());
        assertNull(finalState.getDrawOfferedByUserId());
    }

    @Test
    void repetitionAfterMoveMarksGameOverAndDraw() throws Exception {
        GameService gameService = new GameService();
        ObjectMapper objectMapper = new ObjectMapper();
        ChessWebSocketHandler handler = new ChessWebSocketHandler(gameService, objectMapper);

        List<String> sessionOneMessages = new ArrayList<>();
        List<String> sessionTwoMessages = new ArrayList<>();
        WebSocketSession sessionOne = mockSession("session-1", sessionOneMessages);
        WebSocketSession sessionTwo = mockSession("session-2", sessionTwoMessages);

        String userOne = "user-one";
        String userTwo = "user-two";

        MatchContext context = initializeMatch(
                handler,
                objectMapper,
                gameService,
                sessionOne,
                sessionTwo,
                sessionOneMessages,
                sessionTwoMessages,
                userOne,
                userTwo);

        playMove(handler, context.whiteSession, context.gameId, context.whiteUserId, "g1", "f3");
        playMove(handler, context.blackSession, context.gameId, context.blackUserId, "g8", "f6");
        playMove(handler, context.whiteSession, context.gameId, context.whiteUserId, "f3", "g1");
        playMove(handler, context.blackSession, context.gameId, context.blackUserId, "f6", "g8");
        playMove(handler, context.whiteSession, context.gameId, context.whiteUserId, "g1", "f3");
        playMove(handler, context.blackSession, context.gameId, context.blackUserId, "g8", "f6");
        playMove(handler, context.whiteSession, context.gameId, context.whiteUserId, "f3", "g1");
        playMove(handler, context.blackSession, context.gameId, context.blackUserId, "f6", "g8");

        GameStateResponse finalState = objectMapper.readValue(
                context.whiteMessages.get(context.whiteMessages.size() - 1),
                GameStateResponse.class);

        assertTrue(finalState.isGameOver());
        assertNotNull(finalState.getGameResult());
        assertEquals(GameResult.DRAW, finalState.getGameResult().getResult());
        assertEquals(EndReason.THREEFOLD_REPETITION, finalState.getGameResult().getEndReason());
        assertNull(finalState.getGameResult().getWinnerUserId());
        assertNull(finalState.getDrawOfferedByUserId());
    }

    @Test
    void promotionMoveUsesRequestedPiece() throws Exception {
        GameService gameService = new GameService();
        ObjectMapper objectMapper = new ObjectMapper();
        ChessWebSocketHandler handler = new ChessWebSocketHandler(gameService, objectMapper);

        List<String> sessionOneMessages = new ArrayList<>();
        List<String> sessionTwoMessages = new ArrayList<>();
        WebSocketSession sessionOne = mockSession("session-1", sessionOneMessages);
        WebSocketSession sessionTwo = mockSession("session-2", sessionTwoMessages);

        MatchContext context = initializeMatch(
                handler,
                objectMapper,
                gameService,
                sessionOne,
                sessionTwo,
                sessionOneMessages,
                sessionTwoMessages,
                "u1",
                "u2");

        Game game = gameService.getGame(context.gameId);
        gameService.getBoard(context.gameId).loadFromFen("1nbq1k2/3P1p2/4p3/8/4P3/r7/P1PP1PPP/RNB1KBNR w KQ - 1 13");
        game.updateFen(gameService.getBoard(context.gameId).getFen());

        handler.handleTextMessage(context.whiteSession,
                new TextMessage(
                        """
                                {"type":"MOVE","gameId":"%s","userId":"%s","from":"d7","to":"c8","promotion":"q"}
                                """.formatted(context.gameId, context.whiteUserId).trim()));

        GameStateResponse moveUpdate = objectMapper.readValue(
                context.whiteMessages.get(context.whiteMessages.size() - 1),
                GameStateResponse.class);

        assertEquals("MOVE", moveUpdate.getType().name());
        assertEquals("d7c8q", moveUpdate.getLastMove());
    }

    @Test
    void invalidPromotionValueReturnsErrorAndKeepsSessionAlive() throws Exception {
        GameService gameService = new GameService();
        ObjectMapper objectMapper = new ObjectMapper();
        ChessWebSocketHandler handler = new ChessWebSocketHandler(gameService, objectMapper);

        List<String> sessionOneMessages = new ArrayList<>();
        List<String> sessionTwoMessages = new ArrayList<>();
        WebSocketSession sessionOne = mockSession("session-1", sessionOneMessages);
        WebSocketSession sessionTwo = mockSession("session-2", sessionTwoMessages);

        MatchContext context = initializeMatch(
                handler,
                objectMapper,
                gameService,
                sessionOne,
                sessionTwo,
                sessionOneMessages,
                sessionTwoMessages,
                "u1",
                "u2");

        Game game = gameService.getGame(context.gameId);
        gameService.getBoard(context.gameId).loadFromFen("1nbq1k2/3P1p2/4p3/8/4P3/r7/P1PP1PPP/RNB1KBNR w KQ - 1 13");
        game.updateFen(gameService.getBoard(context.gameId).getFen());

        int before = context.whiteMessages.size();

        handler.handleTextMessage(context.whiteSession,
                new TextMessage(
                        """
                                {"type":"MOVE","gameId":"%s","userId":"%s","from":"d7","to":"c8","promotion":"king"}
                                """.formatted(context.gameId, context.whiteUserId).trim()));

        assertEquals(before + 1, context.whiteMessages.size());

        GameStatusResponse error = objectMapper.readValue(
                context.whiteMessages.get(context.whiteMessages.size() - 1),
                GameStatusResponse.class);

        assertEquals("ERROR", error.getType().name());
        assertEquals("promotion must be one of: q, r, b, n", error.getMessage());
    }

    private MatchContext initializeMatch(
            ChessWebSocketHandler handler,
            ObjectMapper objectMapper,
            GameService gameService,
            WebSocketSession sessionOne,
            WebSocketSession sessionTwo,
            List<String> sessionOneMessages,
            List<String> sessionTwoMessages,
            String userOne,
            String userTwo) throws Exception {
        handler.handleTextMessage(sessionOne,
                new TextMessage("""
                        {"type":"INIT_GAME","userId":"%s"}
                        """.formatted(userOne).trim()));
        assertEquals(0, sessionOneMessages.size());

        handler.handleTextMessage(sessionTwo,
                new TextMessage("""
                        {"type":"INIT_GAME","userId":"%s"}
                        """.formatted(userTwo).trim()));

        assertEquals(1, sessionOneMessages.size());
        assertEquals(1, sessionTwoMessages.size());

        GameStatusResponse matchedOne = objectMapper.readValue(sessionOneMessages.get(0), GameStatusResponse.class);
        GameStatusResponse matchedTwo = objectMapper.readValue(sessionTwoMessages.get(0), GameStatusResponse.class);
        String gameId = matchedOne.getGameId();
        assertNotNull(gameService.getGame(gameId));

        Map<String, String> userIdByColor = "WHITE".equals(matchedOne.getMessage())
                ? Map.of("WHITE", userOne, "BLACK", userTwo)
                : Map.of("WHITE", userTwo, "BLACK", userOne);

        WebSocketSession whiteSession = "WHITE".equals(matchedOne.getMessage()) ? sessionOne : sessionTwo;
        WebSocketSession blackSession = "WHITE".equals(matchedOne.getMessage()) ? sessionTwo : sessionOne;
        List<String> whiteMessages = "WHITE".equals(matchedOne.getMessage()) ? sessionOneMessages : sessionTwoMessages;
        List<String> blackMessages = "WHITE".equals(matchedOne.getMessage()) ? sessionTwoMessages : sessionOneMessages;

        return new MatchContext(
                gameId,
                userIdByColor.get("WHITE"),
                userIdByColor.get("BLACK"),
                whiteSession,
                blackSession,
                whiteMessages,
                blackMessages);
    }

    private void playMove(
            ChessWebSocketHandler handler,
            WebSocketSession session,
            String gameId,
            String userId,
            String from,
            String to) throws Exception {
        handler.handleTextMessage(session,
                new TextMessage("""
                        {"type":"MOVE","gameId":"%s","userId":"%s","from":"%s","to":"%s"}
                        """.formatted(gameId, userId, from, to).trim()));
    }

    private String resolveColorMessage(String firstMessage, String secondMessage, ObjectMapper objectMapper) throws Exception {
        GameStatusResponse first = objectMapper.readValue(firstMessage, GameStatusResponse.class);
        GameStatusResponse second = objectMapper.readValue(secondMessage, GameStatusResponse.class);
        return "WHITE".equals(first.getMessage()) ? first.getMessage() : second.getMessage();
    }

    private WebSocketSession mockSession(String sessionId, List<String> messages) throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.isOpen()).thenReturn(true);
        doAnswer(invocation -> {
            TextMessage message = invocation.getArgument(0);
            messages.add(message.getPayload());
            return null;
        }).when(session).sendMessage(any());
        return session;
    }
}


