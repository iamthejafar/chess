package com.gbkl.Chess.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DrawResponsePayload extends BasePayload {
    private String gameId;
    private String playerUid;
    private boolean accepted;

    public DrawResponsePayload() {
        setMessage("DRAW_RESPONSE");
    }
}
