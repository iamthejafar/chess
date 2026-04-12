package com.jafar.chess.service;

import com.jafar.chess.model.Game;
import com.jafar.chess.model.MatchResult;
import com.jafar.chess.shared.EndReason;
import com.jafar.chess.shared.GameResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {

    @Test
    void findGamesByUserIdReturnsPaginatedGamesForBothColors() {
        GameService gameService = new GameService();
        createGame(gameService, "u1", "u2");
        createGame(gameService, "u3", "u1");
        createGame(gameService, "u4", "u1");
        createGame(gameService, "x1", "x2");

        GameService.UserGamesPage firstPage = gameService.findGamesByUserId("u1", 0, 2);
        GameService.UserGamesPage secondPage = gameService.findGamesByUserId("u1", 1, 2);

        assertEquals(3, firstPage.totalElements());
        assertEquals(2, firstPage.totalPages());
        assertTrue(firstPage.hasNext());
        assertEquals(2, firstPage.games().size());
        assertTrue(firstPage.games().stream().allMatch(game -> game.isParticipant("u1")));

        assertEquals(1, secondPage.games().size());
        assertFalse(secondPage.hasNext());
        assertTrue(secondPage.games().stream().allMatch(game -> game.isParticipant("u1")));
    }

    @Test
    void findGamesByUserIdValidatesPageAndSize() {
        GameService gameService = new GameService();

        IllegalArgumentException pageError = assertThrows(IllegalArgumentException.class,
                () -> gameService.findGamesByUserId("u1", -1, 10));
        IllegalArgumentException sizeError = assertThrows(IllegalArgumentException.class,
                () -> gameService.findGamesByUserId("u1", 0, 0));

        assertEquals("page must be >= 0", pageError.getMessage());
        assertEquals("size must be between 1 and 100", sizeError.getMessage());
    }

    @Test
    void findActiveGameByUserWorksForBothWhiteAndBlack() {
        GameService gameService = new GameService();
        String gameId = createGame(gameService, "u1", "u2");

        Game gameForU1 = gameService.findActiveGameByUserId("u1");
        Game gameForU2 = gameService.findActiveGameByUserId("u2");

        assertEquals(gameId, gameForU1.getGameId());
        assertEquals(gameId, gameForU2.getGameId());
        assertTrue(gameForU1.isParticipant("u1"));
        assertTrue(gameForU2.isParticipant("u2"));
    }

    @Test
    void findActiveGameByUserRequiresUserId() {
        GameService gameService = new GameService();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> gameService.findActiveGameByUserId("  "));

        assertEquals("userId is required", error.getMessage());
    }

    @Test
    void findActiveGameByUserFailsWhenGameAlreadyFinished() {
        GameService gameService = new GameService();
        String gameId = createGame(gameService, "u1", "u2");
        gameService.resign("u1", gameId);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> gameService.findActiveGameByUserId("u1"));

        assertEquals("No active game found for user", error.getMessage());
    }

    @Test
    void resignMarksGameOverAndSetsWinner() {
        GameService gameService = new GameService();
        String gameId = createGame(gameService, "u1", "u2");

        Game updated = gameService.resign("u1", gameId);

        assertTrue(updated.isGameOver());
        assertEquals(GameResult.RESIGNATION, updated.getResult());
        assertEquals(EndReason.RESIGNATION, updated.getEndReason());
        assertEquals("u2", updated.getWinnerUserId());
        assertFalse(updated.isDrawOffered());
        assertNull(updated.getDrawOfferedByUserId());
    }

    @Test
    void drawOfferAndAcceptEndsGameAsDraw() {
        GameService gameService = new GameService();
        String gameId = createGame(gameService, "u1", "u2");

        Game offered = gameService.offerDraw("u1", gameId);
        assertTrue(offered.isDrawOffered());
        assertEquals("u1", offered.getDrawOfferedByUserId());

        Game accepted = gameService.acceptDraw("u2", gameId);
        assertTrue(accepted.isGameOver());
        assertEquals(GameResult.DRAW, accepted.getResult());
        assertEquals(EndReason.MUTUAL_AGREEMENT, accepted.getEndReason());
        assertNull(accepted.getWinnerUserId());
        assertFalse(accepted.isDrawOffered());
        assertNull(accepted.getDrawOfferedByUserId());
    }

    @Test
    void drawDeclineClearsOfferAndKeepsGameInProgress() {
        GameService gameService = new GameService();
        String gameId = createGame(gameService, "u1", "u2");

        gameService.offerDraw("u1", gameId);
        Game declined = gameService.declineDraw("u2", gameId);

        assertFalse(declined.isGameOver());
        assertEquals(GameResult.IN_PROGRESS, declined.getResult());
        assertFalse(declined.isDrawOffered());
        assertNull(declined.getDrawOfferedByUserId());
    }

    @Test
    void acceptDrawWithoutOfferFails() {
        GameService gameService = new GameService();
        String gameId = createGame(gameService, "u1", "u2");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> gameService.acceptDraw("u2", gameId));

        assertEquals("No draw offer to accept", error.getMessage());
    }

    @Test
    void cannotOfferDrawTwiceWhenPending() {
        GameService gameService = new GameService();
        String gameId = createGame(gameService, "u1", "u2");

        gameService.offerDraw("u1", gameId);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> gameService.offerDraw("u2", gameId));

        assertEquals("A draw offer is already pending", error.getMessage());
    }

    @Test
    void cannotAcceptOrDeclineOwnOffer() {
        GameService gameService = new GameService();
        String gameId = createGame(gameService, "u1", "u2");

        gameService.offerDraw("u1", gameId);

        IllegalArgumentException acceptError = assertThrows(IllegalArgumentException.class,
                () -> gameService.acceptDraw("u1", gameId));
        IllegalArgumentException declineError = assertThrows(IllegalArgumentException.class,
                () -> gameService.declineDraw("u1", gameId));

        assertEquals("You cannot accept your own draw offer", acceptError.getMessage());
        assertEquals("You cannot decline your own draw offer", declineError.getMessage());
    }

    @Test
    void nonParticipantCannotUseGameActions() {
        GameService gameService = new GameService();
        String gameId = createGame(gameService, "u1", "u2");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> gameService.resign("u3", gameId));

        assertEquals("User is not part of this game", error.getMessage());
    }

    @Test
    void noActionsAllowedAfterGameOver() {
        GameService gameService = new GameService();
        String gameId = createGame(gameService, "u1", "u2");

        gameService.resign("u1", gameId);

        IllegalArgumentException drawOfferError = assertThrows(IllegalArgumentException.class,
                () -> gameService.offerDraw("u2", gameId));
        IllegalArgumentException resignError = assertThrows(IllegalArgumentException.class,
                () -> gameService.resign("u2", gameId));

        assertEquals("Game is already over", drawOfferError.getMessage());
        assertEquals("Game is already over", resignError.getMessage());
    }

    private String createGame(GameService gameService, String userA, String userB) {
        MatchResult waiting = gameService.requestMatch(userA);
        assertEquals(MatchResult.Status.WAITING, waiting.getStatus());

        MatchResult matched = gameService.requestMatch(userB);
        assertEquals(MatchResult.Status.MATCHED, matched.getStatus());
        assertNotNull(matched.getGameId());
        return matched.getGameId();
    }
}

