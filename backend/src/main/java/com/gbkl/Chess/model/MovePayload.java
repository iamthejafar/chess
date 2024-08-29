package com.gbkl.Chess.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MovePayload extends BasePayload {
    private String from;
    private String to;
    private String gameId;
}
