package com.gbkl.Chess.model;
import com.github.bhlangonijr.chesslib.Board;
import lombok.Data;

@Data
public class GameSession {
    private final Player whitePlayer;
    private final Player blackplayer;
    private final String gameId;
    private final Board board;

    public GameSession(Player whitePlayer, Player blackplayer) {
        this.whitePlayer = whitePlayer;
        this.blackplayer = blackplayer;
        this.gameId = generateGameId();
        this.board = new Board();
    }

    private String generateGameId() {
        return whitePlayer.getUid() + "-" + blackplayer.getUid();
    }
}
