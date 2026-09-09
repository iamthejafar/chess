package com.jafar.chess.dto.request;

import com.jafar.chess.shared.Difficulty;
import com.jafar.chess.shared.GameType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitGameRequest {
    private String userId;
    private GameType gameType; // HUMAN_VS_HUMAN or HUMAN_VS_COMPUTER
    private Difficulty difficulty; // For AI games (EASY, MEDIUM, HARD)
}
