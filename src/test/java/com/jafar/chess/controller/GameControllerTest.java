package com.jafar.chess.controller;

import com.jafar.chess.dto.response.ActiveGameResponse;
import com.jafar.chess.dto.response.AuthResponse;
import com.jafar.chess.dto.response.UserGamesPageResponse;
import com.jafar.chess.model.Game;
import com.jafar.chess.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameControllerTest {

    @Test
    void getUserGamesReturnsPagedHistoryForUser() {
        GameService gameService = mock(GameService.class);
        GameController controller = new GameController(gameService);

        Game gameOne = Game.builder()
                .gameId("g-1")
                .whiteUserId("u1")
                .blackUserId("u2")
                .startTime(LocalDateTime.now())
                .moveCount(12)
                .build();

        Game gameTwo = Game.builder()
                .gameId("g-2")
                .whiteUserId("u3")
                .blackUserId("u1")
                .startTime(LocalDateTime.now().minusMinutes(3))
                .moveCount(22)
                .build();

        when(gameService.findGamesByUserId("u1", 0, 2))
                .thenReturn(new GameService.UserGamesPage(0, 2, 2, 1, false, List.of(gameOne, gameTwo)));

        ResponseEntity<?> response = controller.getUserGames("u1", 0, 2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserGamesPageResponse body = assertInstanceOf(UserGamesPageResponse.class, response.getBody());
        assertEquals(2, body.getTotalElements());
        assertEquals(2, body.getGames().size());
        assertEquals("WHITE", body.getGames().get(0).getUserColor());
        assertEquals("BLACK", body.getGames().get(1).getUserColor());
    }

    @Test
    void getUserGamesReturnsBadRequestForInvalidPagination() {
        GameService gameService = mock(GameService.class);
        GameController controller = new GameController(gameService);

        when(gameService.findGamesByUserId("u1", -1, 10))
                .thenThrow(new IllegalArgumentException("page must be >= 0"));

        ResponseEntity<?> response = controller.getUserGames("u1", -1, 10);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        AuthResponse body = assertInstanceOf(AuthResponse.class, response.getBody());
        assertEquals("page must be >= 0", body.getError());
    }

    @Test
    void getActiveGameByUserReturnsGameWhenUserIsWhite() {
        GameService gameService = mock(GameService.class);
        GameController controller = new GameController(gameService);

        Game game = Game.builder()
                .gameId("g-1")
                .whiteUserId("white-user")
                .blackUserId("black-user")
                .fen("start-fen")
                .moveCount(0)
                .moves(List.of())
                .startTime(LocalDateTime.now())
                .build();

        when(gameService.findActiveGameByUserId("white-user")).thenReturn(game);

        ResponseEntity<?> response = controller.getActiveGameByUser("white-user");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ActiveGameResponse body = assertInstanceOf(ActiveGameResponse.class, response.getBody());
        assertEquals("WHITE", body.getUserColor());
        assertEquals("black-user", body.getOpponentUserId());
        assertEquals("g-1", body.getGameId());
    }

    @Test
    void getActiveGameByUserReturnsGameWhenUserIsBlack() {
        GameService gameService = mock(GameService.class);
        GameController controller = new GameController(gameService);

        Game game = Game.builder()
                .gameId("g-2")
                .whiteUserId("white-user")
                .blackUserId("black-user")
                .fen("start-fen")
                .moveCount(4)
                .moves(List.of("e2e4", "e7e5"))
                .startTime(LocalDateTime.now())
                .build();

        when(gameService.findActiveGameByUserId("black-user")).thenReturn(game);

        ResponseEntity<?> response = controller.getActiveGameByUser("black-user");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ActiveGameResponse body = assertInstanceOf(ActiveGameResponse.class, response.getBody());
        assertEquals("BLACK", body.getUserColor());
        assertEquals("white-user", body.getOpponentUserId());
        assertEquals("g-2", body.getGameId());
        assertNotNull(body.getMoves());
    }

    @Test
    void getActiveGameByUserReturnsBadRequestForInvalidInput() {
        GameService gameService = mock(GameService.class);
        GameController controller = new GameController(gameService);

        when(gameService.findActiveGameByUserId(" "))
                .thenThrow(new IllegalArgumentException("userId is required"));

        ResponseEntity<?> response = controller.getActiveGameByUser(" ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        AuthResponse body = assertInstanceOf(AuthResponse.class, response.getBody());
        assertEquals("userId is required", body.getError());
    }

    @Test
    void getActiveGameByUserReturnsNotFoundWhenNoGameExists() {
        GameService gameService = mock(GameService.class);
        GameController controller = new GameController(gameService);

        when(gameService.findActiveGameByUserId("u-missing"))
                .thenThrow(new RuntimeException("No active game found for user"));

        ResponseEntity<?> response = controller.getActiveGameByUser("u-missing");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        AuthResponse body = assertInstanceOf(AuthResponse.class, response.getBody());
        assertEquals("No active game found for user", body.getError());
    }
}

