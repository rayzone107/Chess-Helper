package com.rachitgoyal.chesshelper.domain.chess.model

enum class Side {
    WHITE,
    BLACK;

    fun opposite(): Side = if (this == WHITE) BLACK else WHITE

    val displayName: String
        get() = if (this == WHITE) "White" else "Black"
}

