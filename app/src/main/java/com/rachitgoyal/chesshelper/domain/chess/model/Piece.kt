package com.rachitgoyal.chesshelper.domain.chess.model

enum class PieceType(val materialValue: Int) {
    KING(20_000),
    QUEEN(900),
    ROOK(500),
    BISHOP(330),
    KNIGHT(320),
    PAWN(100)
}

data class Piece(
    val side: Side,
    val type: PieceType,
) {
    val symbol: String
        get() = when (type) {
            PieceType.KING -> "♚"
            PieceType.QUEEN -> "♛"
            PieceType.ROOK -> "♜"
            PieceType.BISHOP -> "♝"
            PieceType.KNIGHT -> "♞"
            PieceType.PAWN -> "♟"
        }
}

data class CastlingRights(
    val whiteKingSide: Boolean = true,
    val whiteQueenSide: Boolean = true,
    val blackKingSide: Boolean = true,
    val blackQueenSide: Boolean = true,
)

