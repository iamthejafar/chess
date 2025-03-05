package com.gbkl.Chess.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class JoinPayload extends BasePayload {
    private String uid;
    private String name;

    public JoinPayload() {
        setMessage("JOIN");
    }
}
