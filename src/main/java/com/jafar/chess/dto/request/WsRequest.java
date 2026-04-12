package com.jafar.chess.dto.request;

import com.jafar.chess.shared.Messages;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class WsRequest {
    private Messages type;
}
