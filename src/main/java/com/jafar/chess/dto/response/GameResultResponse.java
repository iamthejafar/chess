package com.jafar.chess.dto.response;


import com.jafar.chess.shared.EndReason;
import com.jafar.chess.shared.GameResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameResultResponse {

    /**
     * High-level outcome of the game (e.g. CHECKMATE, DRAW, TIMEOUT).
     */
    private GameResult result;

    /**
     * Specific reason the game ended (e.g. STALEMATE, RESIGNATION).
     * Provides finer granularity than {@link GameResult}.
     */
    private EndReason endReason;

    /**
     * userId of the winner. Null when the result is a draw.
     */
    private String winnerUserId;
}