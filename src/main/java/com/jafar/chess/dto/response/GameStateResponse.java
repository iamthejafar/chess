package com.jafar.chess.dto.response;

import com.jafar.chess.shared.Messages;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStateResponse {

    private Messages type;
    private String gameId;

    private String whiteUserId;
    private String blackUserId;

    private Integer moveCount;
    private String fen;
    private String lastMove;
    private List<String> moves;


    private boolean gameOver;
    private GameResultResponse gameResult;

    private boolean drawOffered;
    private String drawOfferedByUserId;
}