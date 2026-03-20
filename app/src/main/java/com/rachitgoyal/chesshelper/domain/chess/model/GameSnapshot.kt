package com.rachitgoyal.chesshelper.domain.chess.model

data class GameSnapshot(
    val position: ChessPosition,
    val selectedSquare: String?,
    val legalTargets: Set<String>,
    val lastMove: MoveRecord?,
    val moveHistory: List<MoveRecord>,
    val gameStatus: GameStatus,
    val checkedKingSquare: String?,
) {
    val board: Map<String, Piece>
        get() = position.board

    val sideToMove: Side
        get() = position.sideToMove

    val isGameOver: Boolean
        get() = gameStatus.isTerminal
}

