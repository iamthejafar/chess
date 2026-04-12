package com.jafar.chess.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MatchResult {
    public enum Status {
        WAITING,
        MATCHED
    }

    private final Status status;
    private final String userId;
    private final String opponentUserId;
    private final String gameId;
}
