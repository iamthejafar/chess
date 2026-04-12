package com.jafar.chess.dto.request;

import com.jafar.chess.shared.Messages;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoveRequest {
    private String gameId;
    private String userId;
    private String from;
    private String to;
    private String promotion;
}
