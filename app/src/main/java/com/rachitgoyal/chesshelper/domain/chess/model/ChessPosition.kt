package com.rachitgoyal.chesshelper.domain.chess.model

data class ChessPosition(
    val board: Map<String, Piece>,
    val sideToMove: Side,
    val castlingRights: CastlingRights = CastlingRights(),
    val enPassantTarget: String? = null,
)

