package com.jafar.chess.dto.response;

import com.jafar.chess.shared.Messages;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStatusResponse {
    private Messages type;
    private String message;
    private String userId;
    private String sessionId;
    private String opponentUserId;
    private String gameId;
}
