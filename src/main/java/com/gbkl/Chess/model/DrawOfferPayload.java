package com.gbkl.Chess.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DrawOfferPayload extends BasePayload {
    private String gameId;
    private String playerUid;

    public DrawOfferPayload() {
        setMessage("DRAW_OFFER");
    }
}
