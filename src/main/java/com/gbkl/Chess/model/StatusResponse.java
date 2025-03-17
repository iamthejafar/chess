package com.gbkl.Chess.model;

import lombok.Data;

@Data
public class StatusResponse {
    private final String type = "STATUS";
    private final String message;

    public StatusResponse(String message) {
        this.message = message;
    }
}
