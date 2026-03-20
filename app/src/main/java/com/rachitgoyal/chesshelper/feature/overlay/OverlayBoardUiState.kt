package com.rachitgoyal.chesshelper.feature.overlay

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.rachitgoyal.chesshelper.domain.chess.model.GameStatus
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
    val gameStatus: GameStatus = GameStatus.NORMAL,
    val checkedKingSquare: String? = null,
    val assistedSide: Side = Side.BLACK,
    val recommendationState: RecommendationState = RecommendationState.IDLE,
    val recommendation: EngineRecommendation? = null,
    val isRecommendationStale: Boolean = false,
    val recommendationStatusLabel: String? = null,
    val recommendationError: String? = null,
    /** Mirrors [AppSettings.autoApplyBestMove]. Refreshed each time a recommendation is requested. */
    val autoApplyBestMove: Boolean = true,
) {
    val isGameOver: Boolean
        get() = gameStatus.isTerminal

    val canUndo: Boolean
        get() = moveHistory.isNotEmpty()


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

    val recommendationBannerText: String
        get() {
            val activeMove = activeRecommendedMove
            val recommendationSummary = recommendation?.summary
            return when {
                recommendationState == RecommendationState.ERROR && recommendationError != null -> recommendationError
                activeMove != null && recommendationSummary != null -> recommendationSummary
                gameStatus == GameStatus.CHECKMATE -> "Checkmate"
                gameStatus == GameStatus.STALEMATE -> "Stalemate"
                gameStatus == GameStatus.CHECK -> "Check"
                recommendationState == RecommendationState.LOADING -> "Analyzing…"
                else -> compactStatusText
            }
        }

    val isRecommendationBannerError: Boolean
        get() = recommendationState == RecommendationState.ERROR && recommendationStatusLabel == "Engine error"

    val compactStatusText: String
        get() {
            val activeMove = activeRecommendedMove
            val statusLabel = recommendationStatusLabel
            return when {
                gameStatus == GameStatus.CHECKMATE -> "Checkmate"
                gameStatus == GameStatus.STALEMATE -> "Stalemate"
                gameStatus == GameStatus.CHECK && recommendationState == RecommendationState.LOADING -> "Check • Analyzing…"
                gameStatus == GameStatus.CHECK && activeMove != null -> "Check • Best: ${activeMove.notation}"
                gameStatus == GameStatus.CHECK && recommendation != null && isRecommendationStale -> "Check • Best: ${recommendation.move.notation} • stale"
                gameStatus == GameStatus.CHECK && statusLabel != null -> "Check • $statusLabel"
                gameStatus == GameStatus.CHECK -> "Check"
                recommendationState == RecommendationState.LOADING -> "Analyzing…"
                statusLabel != null -> statusLabel
                recommendationError != null -> "No move"
                activeMove != null -> "Best: ${activeMove.notation}"
                recommendation != null && isRecommendationStale -> "Best: ${recommendation.move.notation} • stale"
                else -> "Ready"
            }
        }
}

