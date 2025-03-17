

package com.gbkl.Chess.model;

import com.gbkl.Chess.comman.Enums;
import lombok.Data;

@Data
public class ChessPiece {
    private Enums.ChessPieceType type;
    private String color;
}
