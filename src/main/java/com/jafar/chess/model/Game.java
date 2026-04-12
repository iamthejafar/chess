package com.jafar.chess.model;

import com.jafar.chess.shared.EndReason;
import com.jafar.chess.shared.GameResult;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "gameId")
@ToString(exclude = "moves")
public class Game {

    @Id
    @Column(nullable = false, updatable = false)
    private String gameId;


    @Column(nullable = false, updatable = false)
    private String whiteUserId;

    @Column(nullable = false, updatable = false)
    private String blackUserId;

    // ── Board state ───────────────────────────────────────────────────────────────

    @Column(columnDefinition = "TEXT")
    private String fen;

    private Integer moveCount;

    @ElementCollection
    @CollectionTable(name = "game_moves", joinColumns = @JoinColumn(name = "game_id"))
    @OrderColumn(name = "move_index")
    @Column(name = "move", nullable = false)
    @Builder.Default
    private List<String> moves = new ArrayList<>();


    @Column(nullable = false, updatable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;


    @Builder.Default
    private boolean gameOver = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GameResult result = GameResult.IN_PROGRESS;

    @Enumerated(EnumType.STRING)
    private EndReason endReason;

    private String winnerUserId;


    @Builder.Default
    private boolean drawOffered = false;

    private String drawOfferedByUserId;


    public void updateFen(String fen) {
        if (fen == null || fen.isBlank()) {
            return;
        }
        this.fen = fen;
    }

    public void addMove(String move) {
        if (move == null || move.isBlank()) {
            return;
        }
        moves.add(move);
        moveCount = moves.size();           // keep moveCount in sync automatically
    }

    /**
     * Ends the game, setting all terminal fields atomically.
     * Prefer this over scattering individual setters across service code.
     */
    public void conclude(GameResult result, EndReason endReason, String winnerUserId) {
        this.gameOver = true;
        this.result = result;
        this.endReason = endReason;
        this.winnerUserId = winnerUserId;
        this.endTime = LocalDateTime.now();
        this.drawOffered = false;
        this.drawOfferedByUserId = null;
    }

    public void offerDraw(String byUserId) {
        this.drawOffered = true;
        this.drawOfferedByUserId = byUserId;
    }

    public void retractDrawOffer() {
        this.drawOffered = false;
        this.drawOfferedByUserId = null;
    }


    public boolean isParticipant(String userId) {
        return userId != null
                && (userId.equals(whiteUserId) || userId.equals(blackUserId));
    }

    public String findOpponent(String userId) {
        if (userId == null) return null;
        if (userId.equals(whiteUserId)) return blackUserId;
        if (userId.equals(blackUserId)) return whiteUserId;
        return null;
    }

    public int colorOf(String userId) {
        if (userId == null) return -1;
        if (userId.equals(whiteUserId)) return 0;
        if (userId.equals(blackUserId)) return 1;
        return -1;
    }
}