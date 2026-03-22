package com.rachitgoyal.chesshelper.domain.chess.model

/**
 * A completed (or abandoned) game recorded for match history.
 */
data class MatchRecord(
    val id: String,
    val timestampMillis: Long,
    val moves: List<MoveRecord>,
    val result: MatchResult,
    val assistedSide: Side,
    /** FEN of the starting position. Null means standard initial position. */
    val startingFen: String? = null,
) {
    val moveCount: Int get() = moves.size
}

enum class MatchResult(val label: String) {
    WHITE_WINS("White wins"),
    BLACK_WINS("Black wins"),
    STALEMATE("Stalemate"),
    INCOMPLETE("Incomplete"),
}

