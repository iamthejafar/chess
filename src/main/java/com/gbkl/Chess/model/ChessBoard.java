package com.gbkl.Chess.model;


import lombok.Getter;

@Getter
public class ChessBoard {
    private String[][] board; 

    public ChessBoard() {
        initializeBoard();
    }

    private void initializeBoard() {
        this.board = new String[8][8];
    }

}

