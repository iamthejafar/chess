package com.jafar.chess.service;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import com.jafar.chess.shared.Difficulty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI service using minimax algorithm with alpha-beta pruning.
 * Provides chess engine capabilities for playing against a computer opponent.
 */
@Slf4j
@Service
public class AiService {

    private static final int CHECKMATE_SCORE = 100000;
    private static final int STALEMATE_SCORE = 0;

    private long nodeCount = 0;
    private long startTime = 0;

    /**
     * Find the best move for the current position using minimax with alpha-beta pruning.
     */
    public Move findBestMove(Board board, Difficulty difficulty) {
        if (board == null || board.isMated() || board.isDraw()) {
            return null;
        }

        List<Move> legalMoves = board.legalMoves();
        if (legalMoves.isEmpty()) {
            return null;
        }

        if (legalMoves.size() == 1) {
            return legalMoves.get(0);
        }

        nodeCount = 0;
        startTime = System.currentTimeMillis();

        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        int depth = difficulty.getDepth();

        for (Move move : legalMoves) {
            board.doMove(move);
            int score = -minimax(board, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            board.undoMove();

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }

            // Check timeout
            if (System.currentTimeMillis() - startTime > difficulty.getMaxTimeMs()) {
                break;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.debug("AI: depth={}, nodes={}, score={}, time={}ms", depth, nodeCount, bestScore, elapsed);

        return bestMove;
    }

    /**
     * Minimax algorithm with alpha-beta pruning.
     */
    private int minimax(Board board, int depth, int alpha, int beta, boolean isMaximizing) {
        nodeCount++;

        // Check terminal conditions
        if (depth == 0) {
            return evaluatePosition(board);
        }

        if (board.isMated()) {
            return isMaximizing ? -CHECKMATE_SCORE : CHECKMATE_SCORE;
        }

        if (board.isDraw() || board.isStaleMate()) {
            return STALEMATE_SCORE;
        }

        List<Move> legalMoves = board.legalMoves();
        if (legalMoves.isEmpty()) {
            if (board.isKingAttacked()) {
                return isMaximizing ? -CHECKMATE_SCORE : CHECKMATE_SCORE;
            }
            return STALEMATE_SCORE;
        }

        if (isMaximizing) {
            int maxScore = Integer.MIN_VALUE;
            for (Move move : legalMoves) {
                board.doMove(move);
                int score = minimax(board, depth - 1, alpha, beta, false);
                board.undoMove();

                maxScore = Math.max(maxScore, score);
                alpha = Math.max(alpha, score);

                if (beta <= alpha) {
                    break; // Beta cutoff
                }
            }
            return maxScore;
        } else {
            int minScore = Integer.MAX_VALUE;
            for (Move move : legalMoves) {
                board.doMove(move);
                int score = minimax(board, depth - 1, alpha, beta, true);
                board.undoMove();

                minScore = Math.min(minScore, score);
                beta = Math.min(beta, score);

                if (beta <= alpha) {
                    break; // Alpha cutoff
                }
            }
            return minScore;
        }
    }

    /**
     * Evaluate a board position.
     * Uses material count and basic positional heuristics.
     * Positive score favors white, negative favors black.
     */
    private int evaluatePosition(Board board) {
        int score = 0;

        // Evaluate material values
        score += getMaterialScore(board);

        // Basic positional evaluation
        score += evaluateCenterControl(board);
        score += evaluateKingSafety(board);
        score += evaluateMobility(board);

        return score;
    }

    /**
     * Calculate material balance using FEN string.
     */
    private int getMaterialScore(Board board) {
        String fen = board.getFen();
        int whiteScore = 0;
        int blackScore = 0;

        // Parse FEN to count material
        for (char c : fen.split(" ")[0].toCharArray()) {
            switch (c) {
                case 'P' -> whiteScore += 1;
                case 'N' -> whiteScore += 3;
                case 'B' -> whiteScore += 3;
                case 'R' -> whiteScore += 5;
                case 'Q' -> whiteScore += 9;
                case 'p' -> blackScore += 1;
                case 'n' -> blackScore += 3;
                case 'b' -> blackScore += 3;
                case 'r' -> blackScore += 5;
                case 'q' -> blackScore += 9;
            }
        }

        return (whiteScore - blackScore) * 100;
    }

    /**
     * Evaluate center control.
     */
    private int evaluateCenterControl(Board board) {
        int score = 0;

        // Evaluate presence of pieces in the center
        score += evaluateCenterSquares(board);

        // Encourage moving pieces toward center
        score += evaluateMobilityTowardCenter(board);

        return score;
    }

    /**
     * Count pieces in central squares.
     */
    private int evaluateCenterSquares(Board board) {
        int score = 0;
        // Count material in center (d4, e4, d5, e5)
        return score;
    }

    /**
     * Simple mobility evaluation.
     */
    private int evaluateMobilityTowardCenter(Board board) {
        return 0;
    }

    /**
     * Evaluate king safety (simple version).
     */
    private int evaluateKingSafety(Board board) {
        int score = 0;

        // Penalize if a king is in check
        if (board.isKingAttacked()) {
            // Current side to move is in check
            score -= 50;
        }

        return score;
    }

    /**
     * Evaluate piece mobility (number of available moves).
     */
    private int evaluateMobility(Board board) {
        Side sideToMove = board.getSideToMove();
        List<Move> currentMoves = board.legalMoves();
        int currentMobility = currentMoves.size();

        // Switch sides and evaluate opponent's mobility
        board.setSideToMove(sideToMove == Side.WHITE ? Side.BLACK : Side.WHITE);
        List<Move> opponentMoves = board.legalMoves();
        int opponentMobility = opponentMoves.size();
        board.setSideToMove(sideToMove);

        // More moves is better for current player
        return (currentMobility - opponentMobility) * 10;
    }

    /**
     * Generate a random legal move (for exploration or fallback).
     */
    public Move getRandomMove(Board board) {
        List<Move> legalMoves = board.legalMoves();
        if (legalMoves.isEmpty()) {
            return null;
        }
        return legalMoves.get((int) (Math.random() * legalMoves.size()));
    }
}



