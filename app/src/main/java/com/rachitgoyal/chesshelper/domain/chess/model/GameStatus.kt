package com.rachitgoyal.chesshelper.domain.chess.model

enum class GameStatus {
    NORMAL,
    CHECK,
    CHECKMATE,
    STALEMATE;

    val isTerminal: Boolean
        get() = this == CHECKMATE || this == STALEMATE
}
