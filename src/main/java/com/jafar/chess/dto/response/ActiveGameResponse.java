package com.jafar.chess.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveGameResponse {

    private String userId;
    private String userColor;
    private String opponentUserId;

    private String gameId;
    private String whiteUserId;
    private String blackUserId;

    private Integer moveCount;
    private String fen;
    private List<String> moves;

    private boolean drawOffered;
    private String drawOfferedByUserId;

    private boolean gameOver;
    private GameResultResponse gameResult;
}

