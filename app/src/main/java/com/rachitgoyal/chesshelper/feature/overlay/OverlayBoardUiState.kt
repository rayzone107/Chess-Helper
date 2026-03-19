package com.rachitgoyal.chesshelper.feature.overlay

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.rachitgoyal.chesshelper.domain.chess.model.MoveRecord
import com.rachitgoyal.chesshelper.domain.chess.model.Piece
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import com.rachitgoyal.chesshelper.engine.model.EngineRecommendation

enum class PanelMode {
    EXPANDED,
    MINIMIZED,
}

enum class RecommendationState {
    IDLE,
    LOADING,
    READY,
    ERROR,
}

data class OverlayBoardUiState(
    val panelMode: PanelMode = PanelMode.EXPANDED,
    val panelOffsetPx: IntOffset = IntOffset(32, 80),
    val isDragging: Boolean = false,
    val windowBoundsPx: IntSize = IntSize.Zero,
    val board: Map<String, Piece> = emptyMap(),
    val selectedSquare: String? = null,
    val legalTargets: Set<String> = emptySet(),
    val lastMove: MoveRecord? = null,
    val sideToMove: Side = Side.WHITE,
    val moveHistory: List<MoveRecord> = emptyList(),
    val assistedSide: Side = Side.BLACK,
    val recommendationState: RecommendationState = RecommendationState.IDLE,
    val recommendation: EngineRecommendation? = null,
    val isRecommendationStale: Boolean = false,
    val recommendationError: String? = null,
    val isGameOver: Boolean = false,
) {
    val canUndo: Boolean
        get() = moveHistory.isNotEmpty()

    val playerSide: Side
        get() = assistedSide

    val boardBottomSide: Side
        get() = assistedSide

    val activeRecommendedMove: MoveRecord?
        get() = recommendation?.move?.takeIf { recommendationState == RecommendationState.READY && !isRecommendationStale }

    val canRecommend: Boolean
        get() =
            panelMode == PanelMode.EXPANDED &&
                recommendationState != RecommendationState.LOADING &&
                sideToMove == assistedSide &&
                (moveHistory.isNotEmpty() || assistedSide == Side.WHITE) &&
                activeRecommendedMove == null &&
                !isGameOver

    val canApplyRecommendation: Boolean
        get() = panelMode == PanelMode.EXPANDED && activeRecommendedMove != null && sideToMove == assistedSide

    val compactRecommendationStatus: String
        get() {
            val activeMove = activeRecommendedMove
            return when {
                recommendationState == RecommendationState.LOADING -> "Analyzing…"
                recommendationError != null -> "No move"
                activeMove != null -> "Best: ${activeMove.notation}"
                recommendation != null && isRecommendationStale -> "Best: ${recommendation.move.notation} • stale"
                isGameOver -> "Game over"
                else -> "Ready"
            }
        }
}

