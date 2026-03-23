package com.rachitgoyal.chesshelper.feature.overlay

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.rachitgoyal.chesshelper.domain.chess.model.BoardTheme
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

/** Haptic pulse type emitted after a legal move in the overlay. */
enum class BoardHapticEvent { MOVE, CAPTURE, CHECK }

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
    val isMoveHistoryExpanded: Boolean = false,
    val gameStatus: GameStatus = GameStatus.NORMAL,
    val checkedKingSquare: String? = null,
    val assistedSide: Side = Side.BLACK,
    val recommendationState: RecommendationState = RecommendationState.IDLE,
    val recommendation: EngineRecommendation? = null,
    val isRecommendationStale: Boolean = false,
    val recommendationStatusLabel: String? = null,
    val recommendationError: String? = null,
    /** Mirrors the auto-apply preference. Refreshed each time a recommendation is requested. */
    val autoApplyBestMove: Boolean = true,
    /** Current position as a FEN string; refreshed after every move. */
    val currentFen: String = "",
    /** One-shot flag: set to true to trigger a FEN-copied confirmation in the UI. */
    val fenCopied: Boolean = false,
    /** Mirrors the haptic-feedback preference. */
    val enableHapticFeedback: Boolean = true,
    /** One-shot haptic pulse emitted after a legal move; consumed by the UI layer. */
    val hapticEvent: BoardHapticEvent? = null,
    /** Active board colour theme. */
    val boardTheme: BoardTheme = BoardTheme.CLASSIC,
    /** One-shot sound pulse emitted after a legal move; consumed by the UI layer. */
    val soundEvent: BoardHapticEvent? = null,
    /** Mirrors [AppSettings.enableSoundEffects]. */
    val enableSoundEffects: Boolean = false,
    /** One-shot error message from a failed FEN load; consumed by the UI layer. */
    val fenLoadError: String? = null,

    // ---- Config (board-setup) mode ----
    /** True while the user is freely arranging pieces on the board. */
    val isConfigMode: Boolean = false,
    /** Board snapshot selected in config mode (square id). */
    val configSelectedSquare: String? = null,
    /** Undo stack of board snapshots during config mode. */
    val configUndoStack: List<Map<String, Piece>> = emptyList(),
    /** Redo stack of board snapshots during config mode. */
    val configRedoStack: List<Map<String, Piece>> = emptyList(),
    /** Which side will move once config mode is exited. */
    val configSideToMove: Side = Side.WHITE,

    // ---- Opacity ----
    /** Overall overlay opacity (0.2–1.0). Cascades to the board as well. */
    val overlayOpacity: Float = 1f,
    /** Additional board opacity (0.2–1.0), multiplied with overlay opacity. */
    val boardOpacity: Float = 1f,
) {
    val isGameOver: Boolean
        get() = gameStatus.isTerminal

    val isCheckmate: Boolean
        get() = gameStatus == GameStatus.CHECKMATE

    val canUndo: Boolean
        get() = moveHistory.isNotEmpty()


    val boardBottomSide: Side
        get() = assistedSide

    val activeRecommendedMove: MoveRecord?
        get() = recommendation?.move?.takeIf { recommendationState == RecommendationState.READY && !isRecommendationStale }

    val canRecommend: Boolean
        get() =
            panelMode == PanelMode.EXPANDED &&
                !isConfigMode &&
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
                gameStatus == GameStatus.STALEMATE -> "Stalemate — game over."
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
                gameStatus == GameStatus.CHECKMATE -> "Game over"
                gameStatus == GameStatus.STALEMATE -> "Stalemate • Game over"
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
