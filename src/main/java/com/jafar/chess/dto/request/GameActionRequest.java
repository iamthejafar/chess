package com.jafar.chess.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameActionRequest {
    private String gameId;
    private String userId;
}

