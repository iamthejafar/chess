package com.jafar.chess.controller;

import com.jafar.chess.dto.response.ActiveGameResponse;
import com.jafar.chess.dto.response.AuthResponse;
import com.jafar.chess.dto.response.GameResultResponse;
import com.jafar.chess.dto.response.UserGameSummaryResponse;
import com.jafar.chess.dto.response.UserGamesPageResponse;
import com.jafar.chess.model.Game;
import com.jafar.chess.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping("/active")
    public ResponseEntity<?> getActiveGameByUser(@RequestParam String userId) {
        try {
            Game game = gameService.findActiveGameByUserId(userId);
            return ResponseEntity.ok(toActiveGameResponse(game, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthResponse.error(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AuthResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error while fetching active game", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.error("Failed to fetch active game"));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getUserGames(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            GameService.UserGamesPage result = gameService.findGamesByUserId(userId, page, size);
            List<UserGameSummaryResponse> games = result.games().stream()
                    .map(game -> toUserGameSummary(game, userId))
                    .toList();

            return ResponseEntity.ok(UserGamesPageResponse.builder()
                    .userId(userId)
                    .page(result.page())
                    .size(result.size())
                    .totalElements(result.totalElements())
                    .totalPages(result.totalPages())
                    .hasNext(result.hasNext())
                    .games(games)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error while fetching user games", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.error("Failed to fetch user games"));
        }
    }

    private ActiveGameResponse toActiveGameResponse(Game game, String userId) {
        return ActiveGameResponse.builder()
                .userId(userId)
                .userColor(resolveUserColor(game, userId))
                .opponentUserId(game.findOpponent(userId))
                .gameId(game.getGameId())
                .whiteUserId(game.getWhiteUserId())
                .blackUserId(game.getBlackUserId())
                .moveCount(game.getMoveCount())
                .fen(game.getFen())
                .moves(game.getMoves())
                .drawOffered(game.isDrawOffered())
                .drawOfferedByUserId(game.getDrawOfferedByUserId())
                .gameOver(game.isGameOver())
                .gameResult(toGameResult(game))
                .build();
    }

    private String resolveUserColor(Game game, String userId) {
        if (userId == null || game == null) {
            return null;
        }
        if (userId.equals(game.getWhiteUserId())) {
            return "WHITE";
        }
        if (userId.equals(game.getBlackUserId())) {
            return "BLACK";
        }
        return null;
    }

    private GameResultResponse toGameResult(Game game) {
        if (game == null || !game.isGameOver()) {
            return null;
        }
        return GameResultResponse.builder()
                .result(game.getResult())
                .endReason(game.getEndReason())
                .winnerUserId(game.getWinnerUserId())
                .build();
    }

    private UserGameSummaryResponse toUserGameSummary(Game game, String userId) {
        return UserGameSummaryResponse.builder()
                .gameId(game.getGameId())
                .userColor(resolveUserColor(game, userId))
                .opponentUserId(game.findOpponent(userId))
                .whiteUserId(game.getWhiteUserId())
                .blackUserId(game.getBlackUserId())
                .moveCount(game.getMoveCount())
                .startTime(game.getStartTime())
                .endTime(game.getEndTime())
                .gameOver(game.isGameOver())
                .result(game.getResult())
                .endReason(game.getEndReason())
                .winnerUserId(game.getWinnerUserId())
                .build();
    }
}

