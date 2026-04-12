package com.jafar.chess.shared;

public enum EndReason {
    CHECKMATE,
    RESIGNATION,
    STALEMATE,
    INSUFFICIENT_MATERIAL,
    THREEFOLD_REPETITION,
    FIFTY_MOVE_RULE,
    MUTUAL_AGREEMENT,
    TIMEOUT,
    ABANDONED
}