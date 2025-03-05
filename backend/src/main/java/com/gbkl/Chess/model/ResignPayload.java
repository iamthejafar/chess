package com.gbkl.Chess.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResignPayload extends BasePayload {
    private String gameId;
    private String playerUid;

    public ResignPayload() {
        setMessage("RESIGN");
    }
}
