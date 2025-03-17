package com.gbkl.Chess.model;

import lombok.Data;

@Data
public class GameStateResponse {
    private final String gameId;
    private final String fen;
    private final Player whitePlayer;
    private final Player blackPlayer;
    private final String sideToMove;
    private final boolean isCheck;
    private final boolean isCheckmate;
    private boolean isGameOver;
    private String winner;
    private String endReason;
    private boolean isDrawOffered;
    private String drawOfferedBy;

    public GameStateResponse(String gameId, String fen, Player whitePlayer, Player blackPlayer, 
                           String sideToMove, boolean isCheck, boolean isCheckmate) {
        this.gameId = gameId;
        this.fen = fen;
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.sideToMove = sideToMove;
        this.isCheck = isCheck;
        this.isCheckmate = isCheckmate;
        this.isGameOver = false;
    }
}
