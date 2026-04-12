package com.jafar.chess.dto.response;

import com.jafar.chess.shared.EndReason;
import com.jafar.chess.shared.GameResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGameSummaryResponse {

    private String gameId;
    private String userColor;
    private String opponentUserId;

    private String whiteUserId;
    private String blackUserId;

    private Integer moveCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private boolean gameOver;
    private GameResult result;
    private EndReason endReason;
    private String winnerUserId;
}

