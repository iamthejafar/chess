package com.jafar.chess.shared;

/**
 * Enum for AI difficulty levels.
 */
public enum Difficulty {
    EASY(1, 500),           // Shallow depth, slower calculation
    MEDIUM(3, 2000),        // Medium depth, balanced
    HARD(5, 5000);          // Deep depth, stronger play

    private final int depth;
    private final long maxTimeMs;

    Difficulty(int depth, long maxTimeMs) {
        this.depth = depth;
        this.maxTimeMs = maxTimeMs;
    }

    public int getDepth() {
        return depth;
    }

    public long getMaxTimeMs() {
        return maxTimeMs;
    }
}

