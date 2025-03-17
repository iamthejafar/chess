package com.gbkl.Chess.model;

import lombok.Data;

@Data
public class ErrorResponse {
    private final String type = "ERROR";
    private final String message;

    public ErrorResponse(String message) {
        this.message = message;
    }
}
