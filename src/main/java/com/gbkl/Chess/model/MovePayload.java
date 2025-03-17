package com.gbkl.Chess.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MovePayload extends BasePayload {
    private String from;
    private String to;
    private String gameId;
    private String playerUid;
    private String promotionPiece; // Optional, for pawn promotion (e.g. "QUEEN", "ROOK", etc.)

    public MovePayload() {
        setMessage("MOVE");
    }
}
