package com.rachitgoyal.chesshelper.domain.chess.model

data class MoveRecord(
    val from: String,
    val to: String,
    val piece: Piece,
    val capturedPiece: Piece? = null,
    val promotion: PieceType? = null,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false,
) {
    val notation: String
        get() = when {
            isCastling && to.startsWith("g") -> "O-O"
            isCastling && to.startsWith("c") -> "O-O-O"
            promotion != null -> "$from → $to=${promotion.name.first()}"
            else -> "$from → $to"
        }
}


