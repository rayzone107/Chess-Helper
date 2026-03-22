package com.rachitgoyal.chesshelper.feature.overlay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import com.rachitgoyal.chesshelper.domain.chess.ChessGameStore
import com.rachitgoyal.chesshelper.domain.chess.model.BoardTheme
import com.rachitgoyal.chesshelper.domain.chess.model.GameSnapshot
import com.rachitgoyal.chesshelper.domain.chess.model.GameStatus
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import com.rachitgoyal.chesshelper.engine.EngineUnavailableException
import com.rachitgoyal.chesshelper.engine.MoveRecommendationEngine
import com.rachitgoyal.chesshelper.engine.model.EngineRecommendation
import com.rachitgoyal.chesshelper.engine.stockfish.ChessPositionFenEncoder
import com.rachitgoyal.chesshelper.settings.AppSettings
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class OverlayBoardViewModel(
    private val store: ChessGameStore = ChessGameStore(),
    private val moveRecommendationEngine: MoveRecommendationEngine,
    private val appSettings: AppSettings? = null,
    private val recommendationExecutor: ExecutorService = Executors.newSingleThreadExecutor(),
    private val uiExecutor: Executor = defaultUiExecutor(),
) : ViewModel() {

    var uiState by mutableStateOf(OverlayBoardUiState())
        private set

    private var panelSizePx: IntSize = IntSize.Zero
    private var recommendationRequestVersion: Int = 0
    private var isDisposed: Boolean = false

    init {
        syncFromStore()
        uiState = uiState.copy(
            autoApplyBestMove = appSettings?.autoApplyBestMove ?: false,
            enableHapticFeedback = appSettings?.enableHapticFeedback ?: true,
            boardTheme = appSettings?.boardTheme ?: BoardTheme.CLASSIC,
            enableSoundEffects = appSettings?.enableSoundEffects ?: false,
        )
    }

    fun onRootBoundsChanged(size: IntSize) {
        uiState = uiState.copy(
            windowBoundsPx = size,
            panelOffsetPx = clampOffset(uiState.panelOffsetPx, size, panelSizePx),
        )
    }

    fun onPanelSizeChanged(size: IntSize) {
        panelSizePx = size
        uiState = uiState.copy(
            panelOffsetPx = clampOffset(uiState.panelOffsetPx, uiState.windowBoundsPx, panelSizePx),
        )
    }

    fun onDragStart() {
        uiState = uiState.copy(isDragging = true)
    }

    fun onDrag(delta: Offset) {
        val nextOffset = IntOffset(
            x = uiState.panelOffsetPx.x + delta.x.roundToInt(),
            y = uiState.panelOffsetPx.y + delta.y.roundToInt(),
        )
        uiState = uiState.copy(
            panelOffsetPx = clampOffset(nextOffset, uiState.windowBoundsPx, panelSizePx),
            isDragging = true,
        )
    }

    fun onDragEnd() {
        uiState = uiState.copy(
            isDragging = false,
            panelOffsetPx = clampOffset(uiState.panelOffsetPx, uiState.windowBoundsPx, panelSizePx),
        )
    }

    fun togglePanelMode() {
        uiState = uiState.copy(
            panelMode = if (uiState.panelMode == PanelMode.EXPANDED) PanelMode.MINIMIZED else PanelMode.EXPANDED,
        )
    }

    fun toggleMoveHistoryExpanded() {
        uiState = uiState.copy(isMoveHistoryExpanded = !uiState.isMoveHistoryExpanded)
    }

    fun onSquareTapped(squareId: String) {
        val moveMade = store.tapSquare(squareId)
        if (moveMade && uiState.recommendationState == RecommendationState.LOADING) {
            recommendationRequestVersion += 1
        }
        val staleRecommendation = if (moveMade && uiState.recommendation != null) true else uiState.isRecommendationStale
        syncFromStore(
            recommendation = if (moveMade && uiState.recommendationState == RecommendationState.LOADING) null else uiState.recommendation,
            recommendationState = when {
                moveMade && uiState.recommendationState == RecommendationState.LOADING -> RecommendationState.IDLE
                moveMade && uiState.recommendation != null -> RecommendationState.READY
                else -> uiState.recommendationState
            },
            isRecommendationStale = staleRecommendation,
            recommendationStatusLabel = null,
            recommendationError = null,
        )
        if (moveMade && uiState.enableHapticFeedback) {
            uiState = uiState.copy(hapticEvent = hapticEventForCurrentState())
        }
        if (moveMade && uiState.enableSoundEffects) {
            uiState = uiState.copy(soundEvent = hapticEventForCurrentState())
        }
    }

    fun onApplyRecommendationClicked() {
        val move = uiState.activeRecommendedMove ?: return
        val applied = store.applyMove(move)
        if (!applied) {
            recommendationRequestVersion += 1
            uiState = uiState.copy(
                recommendationState = RecommendationState.ERROR,
                recommendationStatusLabel = "Suggestion stale",
                recommendationError = "The suggested move is no longer legal in the current position.",
                isRecommendationStale = true,
            )
            return
        }

        syncFromStore(
            recommendation = null,
            recommendationState = RecommendationState.IDLE,
            isRecommendationStale = false,
            recommendationStatusLabel = null,
            recommendationError = null,
        )
        if (uiState.enableHapticFeedback) {
            uiState = uiState.copy(hapticEvent = hapticEventForCurrentState())
        }
        if (uiState.enableSoundEffects) {
            uiState = uiState.copy(soundEvent = hapticEventForCurrentState())
        }
    }

    fun onCopyFenClicked() {
        uiState = uiState.copy(fenCopied = true)
    }

    fun onFenCopyConsumed() {
        uiState = uiState.copy(fenCopied = false)
    }

    fun onHapticConsumed() {
        uiState = uiState.copy(hapticEvent = null)
    }

    fun onSoundEventConsumed() {
        uiState = uiState.copy(soundEvent = null)
    }

    private fun hapticEventForCurrentState(): BoardHapticEvent = when {
        uiState.gameStatus == GameStatus.CHECK || uiState.gameStatus == GameStatus.CHECKMATE -> BoardHapticEvent.CHECK
        uiState.lastMove?.capturedPiece != null -> BoardHapticEvent.CAPTURE
        else -> BoardHapticEvent.MOVE
    }

    fun onUndoClicked() {
        if (!store.undo()) return
        recommendationRequestVersion += 1
        syncFromStore(
            recommendation = null,
            recommendationState = RecommendationState.IDLE,
            isRecommendationStale = false,
            recommendationStatusLabel = null,
            recommendationError = null,
        )
    }

    fun onResetBoard() {
        store.reset()
        recommendationRequestVersion += 1
        syncFromStore(
            recommendation = null,
            recommendationState = RecommendationState.IDLE,
            isRecommendationStale = false,
            recommendationStatusLabel = null,
            recommendationError = null,
        )
        uiState = uiState.copy(isMoveHistoryExpanded = false)
    }

    fun onAssistedSideChanged(side: Side) {
        recommendationRequestVersion += 1
        uiState = uiState.copy(
            assistedSide = side,
            recommendation = null,
            recommendationState = RecommendationState.IDLE,
            isRecommendationStale = false,
            recommendationStatusLabel = null,
            recommendationError = null,
        )
    }

    fun onRecommendClicked() {
        if (!uiState.canRecommend) return
        recommendationRequestVersion += 1
        val requestVersion = recommendationRequestVersion
        val requestedPosition = store.currentPosition()
        val requestedSide = uiState.assistedSide
        val currentAutoApply = appSettings?.autoApplyBestMove ?: false

        uiState = uiState.copy(
            recommendationState = RecommendationState.LOADING,
            recommendation = null,
            isRecommendationStale = false,
            recommendationStatusLabel = null,
            recommendationError = null,
            autoApplyBestMove = currentAutoApply,
        )

        recommendationExecutor.execute {
            val result = try {
                RecommendationRequestResult.Success(
                    recommendation = moveRecommendationEngine.recommend(requestedPosition, requestedSide),
                )
            } catch (exception: EngineUnavailableException) {
                RecommendationRequestResult.EngineFailure(
                    message = exception.message ?: "Stockfish failed to analyze this position. Try again.",
                )
            } catch (_: Exception) {
                RecommendationRequestResult.EngineFailure(
                    message = "Stockfish failed to analyze this position. Try again.",
                )
            }

            uiExecutor.execute {
                if (
                    requestVersion != recommendationRequestVersion ||
                    requestedSide != uiState.assistedSide ||
                    requestedPosition != store.currentPosition()
                ) {
                    return@execute
                }

                uiState = when (result) {
                    is RecommendationRequestResult.Success -> {
                        if (result.recommendation != null) {
                            uiState.copy(
                                recommendationState = RecommendationState.READY,
                                recommendation = result.recommendation,
                                isRecommendationStale = false,
                                recommendationStatusLabel = null,
                                recommendationError = null,
                                autoApplyBestMove = currentAutoApply,
                            ).also {
                                uiState = it
                                // Auto-apply: immediately play the move if the setting is on.
                                if (currentAutoApply) onApplyRecommendationClicked()
                            }
                            // onApplyRecommendationClicked already updated uiState; return early.
                            return@execute
                        } else {
                            uiState.copy(
                                recommendationState = RecommendationState.ERROR,
                                recommendation = null,
                                isRecommendationStale = false,
                                recommendationStatusLabel = "No move",
                                recommendationError = "No legal recommendation available for this position.",
                            )
                        }
                    }

                    is RecommendationRequestResult.EngineFailure -> {
                        uiState.copy(
                            recommendationState = RecommendationState.ERROR,
                            recommendation = null,
                            isRecommendationStale = false,
                            recommendationStatusLabel = "Engine error",
                            recommendationError = result.message,
                        )
                    }
                }
            }
        }
    }

    fun onLoadFen(fen: String) {
        val result = store.loadFen(fen)
        recommendationRequestVersion++
        if (result.isSuccess) {
            syncFromStore(
                recommendation = null,
                recommendationState = RecommendationState.IDLE,
                isRecommendationStale = false,
                recommendationStatusLabel = null,
                recommendationError = null,
            )
            uiState = uiState.copy(fenLoadError = null, isMoveHistoryExpanded = false)
        } else {
            uiState = uiState.copy(fenLoadError = result.exceptionOrNull()?.message ?: "Invalid FEN")
        }
    }

    fun onFenLoadErrorConsumed() {
        uiState = uiState.copy(fenLoadError = null)
    }

    override fun onCleared() {
        dispose()
        super.onCleared()
    }

    fun dispose() {
        if (isDisposed) return
        isDisposed = true
        recommendationExecutor.shutdownNow()
        runCatching { (moveRecommendationEngine as? AutoCloseable)?.close() }
    }

    private fun syncFromStore(
        recommendation: EngineRecommendation? = uiState.recommendation,
        recommendationState: RecommendationState = uiState.recommendationState,
        isRecommendationStale: Boolean = uiState.isRecommendationStale,
        recommendationStatusLabel: String? = uiState.recommendationStatusLabel,
        recommendationError: String? = uiState.recommendationError,
    ) {
        val snapshot: GameSnapshot = store.snapshot()
        uiState = uiState.copy(
            board = snapshot.board,
            selectedSquare = snapshot.selectedSquare,
            legalTargets = snapshot.legalTargets,
            lastMove = snapshot.lastMove,
            sideToMove = snapshot.sideToMove,
            moveHistory = snapshot.moveHistory,
            gameStatus = snapshot.gameStatus,
            checkedKingSquare = snapshot.checkedKingSquare,
            currentFen = ChessPositionFenEncoder.encode(snapshot.position),
            recommendation = recommendation,
            recommendationState = recommendationState,
            isRecommendationStale = isRecommendationStale,
            recommendationStatusLabel = recommendationStatusLabel,
            recommendationError = recommendationError,
        )
    }

    private fun clampOffset(offset: IntOffset, bounds: IntSize, panelSize: IntSize): IntOffset {
        if (bounds == IntSize.Zero || panelSize == IntSize.Zero) return offset
        val maxX = (bounds.width - panelSize.width).coerceAtLeast(0)
        val maxY = (bounds.height - panelSize.height).coerceAtLeast(0)
        return IntOffset(
            x = offset.x.coerceIn(0, maxX),
            y = offset.y.coerceIn(0, maxY),
        )
    }

    companion object {
        private fun defaultUiExecutor(): Executor {
            return try {
                val mainLooper = Looper.getMainLooper()
                if (mainLooper != null) {
                    val handler = Handler(mainLooper)
                    Executor { runnable ->
                        if (Looper.myLooper() == mainLooper) runnable.run() else handler.post(runnable)
                    }
                } else {
                    Executor { runnable -> runnable.run() }
                }
            } catch (_: Throwable) {
                Executor { runnable -> runnable.run() }
            }
        }
    }
}

private sealed interface RecommendationRequestResult {
    data class Success(val recommendation: EngineRecommendation?) : RecommendationRequestResult

    data class EngineFailure(val message: String) : RecommendationRequestResult
}


