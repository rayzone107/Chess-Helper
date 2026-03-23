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
import com.rachitgoyal.chesshelper.data.MatchHistoryRepository
import com.rachitgoyal.chesshelper.domain.chess.ChessRules
import com.rachitgoyal.chesshelper.domain.chess.FenParser
import com.rachitgoyal.chesshelper.domain.chess.ChessGameStore
import com.rachitgoyal.chesshelper.domain.chess.model.BoardTheme
import com.rachitgoyal.chesshelper.domain.chess.model.ChessPosition
import com.rachitgoyal.chesshelper.domain.chess.model.GameSnapshot
import com.rachitgoyal.chesshelper.domain.chess.model.GameStatus
import com.rachitgoyal.chesshelper.domain.chess.model.MatchRecord
import com.rachitgoyal.chesshelper.domain.chess.model.MatchResult
import com.rachitgoyal.chesshelper.domain.chess.model.Piece
import com.rachitgoyal.chesshelper.domain.chess.model.PieceType
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import com.rachitgoyal.chesshelper.engine.EngineUnavailableException
import com.rachitgoyal.chesshelper.engine.MoveRecommendationEngine
import com.rachitgoyal.chesshelper.engine.model.EngineRecommendation
import com.rachitgoyal.chesshelper.engine.stockfish.ChessPositionFenEncoder
import com.rachitgoyal.chesshelper.settings.AppSettings
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class OverlayBoardViewModel(
    private val store: ChessGameStore = ChessGameStore(),
    private val moveRecommendationEngine: MoveRecommendationEngine,
    private val appSettings: AppSettings? = null,
    private val matchHistoryRepository: MatchHistoryRepository? = null,
    private val recommendationExecutor: ExecutorService = Executors.newSingleThreadExecutor(),
    private val uiExecutor: Executor = defaultUiExecutor(),
) : ViewModel() {

    var uiState by mutableStateOf(OverlayBoardUiState())
        private set

    private var panelSizePx: IntSize = IntSize.Zero
    private var recommendationRequestVersion: Int = 0
    private var isDisposed: Boolean = false
    /** Guards [saveCurrentGame] to avoid duplicate entries on game-over + close/reset. */
    private var gameSaved: Boolean = false
    /** Starting FEN for the current game. Null = standard initial position. */
    private var startingFen: String? = null

    init {
        syncFromStore()
        uiState = uiState.copy(
            autoApplyBestMove = appSettings?.autoApplyBestMove ?: false,
            enableHapticFeedback = appSettings?.enableHapticFeedback ?: true,
            boardTheme = appSettings?.boardTheme ?: BoardTheme.CLASSIC,
            enableSoundEffects = appSettings?.enableSoundEffects ?: false,
            overlayOpacity = appSettings?.overlayOpacity ?: 1f,
            boardOpacity = appSettings?.boardOpacity ?: 1f,
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
        val nextMode = if (uiState.panelMode == PanelMode.EXPANDED) PanelMode.MINIMIZED else PanelMode.EXPANDED
        if (nextMode == PanelMode.EXPANDED) refreshOpacity()
        uiState = uiState.copy(panelMode = nextMode)
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
        if (moveMade && uiState.isGameOver) {
            saveCurrentGame()
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
        if (uiState.isGameOver) {
            saveCurrentGame()
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
        saveCurrentGame()
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
        gameSaved = false
        startingFen = null
    }

    // ---- Resume game from history ----

    fun onResumeGame(match: MatchRecord, fromMoveIndex: Int? = null) {
        saveCurrentGame()
        refreshOpacity()
        val startPos = match.startingFen?.let { FenParser.parse(it).getOrNull() }
            ?: ChessRules.initialPosition()
        val movesToReplay = if (fromMoveIndex != null && fromMoveIndex < match.moves.size) {
            match.moves.take(fromMoveIndex)
        } else {
            match.moves
        }
        store.loadGame(startPos, movesToReplay)
        recommendationRequestVersion += 1
        syncFromStore(
            recommendation = null,
            recommendationState = RecommendationState.IDLE,
            isRecommendationStale = false,
            recommendationStatusLabel = null,
            recommendationError = null,
        )
        uiState = uiState.copy(
            assistedSide = match.assistedSide,
            isMoveHistoryExpanded = false,
        )
        gameSaved = false
        startingFen = match.startingFen
    }

    // ---- Config (board-setup) mode ----

    fun onEnterConfigMode() {
        uiState = uiState.copy(
            isConfigMode = true,
            configSelectedSquare = null,
            configUndoStack = emptyList(),
            configRedoStack = emptyList(),
            configSideToMove = uiState.sideToMove,
            configEntryBoard = uiState.board,
            configEntrySideToMove = uiState.sideToMove,
            configCatalogPiece = null,
            configValidationError = null,
        )
    }

    fun onDiscardConfigChanges() {
        if (!uiState.isConfigMode) return
        uiState = uiState.copy(
            isConfigMode = false,
            configSelectedSquare = null,
            configUndoStack = emptyList(),
            configRedoStack = emptyList(),
            configEntryBoard = null,
            configEntrySideToMove = Side.WHITE,
            configCatalogPiece = null,
            configValidationError = null,
        )
        syncFromStore()
    }

    fun onConfigRemoveSelected() {
        val selectedSquare = uiState.configSelectedSquare ?: return
        if (!uiState.board.containsKey(selectedSquare)) {
            uiState = uiState.copy(configSelectedSquare = null)
            return
        }
        val previousBoard = uiState.board
        val nextBoard = previousBoard.toMutableMap().apply { remove(selectedSquare) }
        uiState = uiState.copy(
            board = nextBoard,
            configSelectedSquare = null,
            configUndoStack = uiState.configUndoStack + listOf(previousBoard),
            configRedoStack = emptyList(),
            configValidationError = null,
        )
    }

    fun onExitConfigMode() {
        // Validate the board position
        val board = uiState.board
        val validationError = validateConfigBoard(board)
        if (validationError != null) {
            uiState = uiState.copy(configValidationError = validationError)
            return
        }
        val sideToMove = uiState.configSideToMove
        store.loadBoard(board, sideToMove)
        recommendationRequestVersion += 1
        uiState = uiState.copy(
            isConfigMode = false,
            configSelectedSquare = null,
            configUndoStack = emptyList(),
            configRedoStack = emptyList(),
            configEntryBoard = null,
            configEntrySideToMove = Side.WHITE,
            configCatalogPiece = null,
            configValidationError = null,
        )
        syncFromStore(
            recommendation = null,
            recommendationState = RecommendationState.IDLE,
            isRecommendationStale = false,
            recommendationStatusLabel = null,
            recommendationError = null,
        )
        gameSaved = false
        startingFen = uiState.currentFen
    }

    fun onConfigSquareTapped(squareId: String) {
        val catalogPiece = uiState.configCatalogPiece
        val selected = uiState.configSelectedSquare
        val board = uiState.board.toMutableMap()
        val previousBoard = uiState.board

        // --- Catalog placement mode ---
        if (catalogPiece != null) {
            board[squareId] = catalogPiece
            uiState = uiState.copy(
                board = board,
                configSelectedSquare = null,
                configUndoStack = uiState.configUndoStack + listOf(previousBoard),
                configRedoStack = emptyList(),
                configValidationError = null,
                // Keep catalog piece selected for multiple placements
            )
            return
        }

        // --- Move / remove mode ---
        if (selected == null) {
            // Select a piece if tapped on one
            if (board.containsKey(squareId)) {
                uiState = uiState.copy(configSelectedSquare = squareId, configValidationError = null)
            }
            return
        }

        if (selected == squareId) {
            // Double-tap on selected piece = remove it
            board.remove(squareId)
            uiState = uiState.copy(
                board = board,
                configSelectedSquare = null,
                configUndoStack = uiState.configUndoStack + listOf(previousBoard),
                configRedoStack = emptyList(),
                configValidationError = null,
            )
            return
        }

        // Move piece from selected to tapped square
        val piece = board[selected] ?: run {
            uiState = uiState.copy(configSelectedSquare = null)
            return
        }
        board.remove(selected)
        board[squareId] = piece
        uiState = uiState.copy(
            board = board,
            configSelectedSquare = null,
            configUndoStack = uiState.configUndoStack + listOf(previousBoard),
            configRedoStack = emptyList(),
            configValidationError = null,
        )
    }

    fun onConfigSelectCatalogPiece(piece: Piece?) {
        // Toggle: if same piece is selected again, deselect
        uiState = uiState.copy(
            configCatalogPiece = if (uiState.configCatalogPiece == piece) null else piece,
            configSelectedSquare = null,
            configValidationError = null,
        )
    }

    fun onConfigDismissValidationError() {
        uiState = uiState.copy(configValidationError = null)
    }

    private fun validateConfigBoard(board: Map<String, Piece>): String? {
        val whiteKings = board.values.count { it.side == Side.WHITE && it.type == PieceType.KING }
        val blackKings = board.values.count { it.side == Side.BLACK && it.type == PieceType.KING }
        if (whiteKings != 1) return "White must have exactly one king (found $whiteKings)."
        if (blackKings != 1) return "Black must have exactly one king (found $blackKings)."

        // No pawns on rank 1 or rank 8
        for ((sq, p) in board) {
            if (p.type == PieceType.PAWN) {
                val rank = sq[1].digitToInt()
                if (rank == 1 || rank == 8) return "Pawns cannot be on rank $rank ($sq)."
            }
        }

        // The side NOT to move must not be in check
        val sideToMove = uiState.configSideToMove
        val opponent = sideToMove.opposite()
        val opponentKingSq = board.entries.find { it.value.side == opponent && it.value.type == PieceType.KING }?.key
        if (opponentKingSq != null) {
            val tempPosition = ChessPosition(board = board, sideToMove = sideToMove)
            if (ChessRules.isSquareAttacked(tempPosition, opponentKingSq, sideToMove)) {
                return "${opponent.displayName}'s king is in check, but it's ${sideToMove.displayName}'s turn."
            }
        }

        return null
    }

    fun onConfigUndo() {
        val stack = uiState.configUndoStack
        if (stack.isEmpty()) return
        val previousBoard = stack.last()
        uiState = uiState.copy(
            board = previousBoard,
            configUndoStack = stack.dropLast(1),
            configRedoStack = uiState.configRedoStack + listOf(uiState.board),
            configSelectedSquare = null,
            configCatalogPiece = null,
            configValidationError = null,
        )
    }

    fun onConfigRedo() {
        val stack = uiState.configRedoStack
        if (stack.isEmpty()) return
        val nextBoard = stack.last()
        uiState = uiState.copy(
            board = nextBoard,
            configRedoStack = stack.dropLast(1),
            configUndoStack = uiState.configUndoStack + listOf(uiState.board),
            configSelectedSquare = null,
            configCatalogPiece = null,
            configValidationError = null,
        )
    }

    fun onConfigClearBoard() {
        val previousBoard = uiState.board
        uiState = uiState.copy(
            board = emptyMap(),
            configSelectedSquare = null,
            configUndoStack = uiState.configUndoStack + listOf(previousBoard),
            configRedoStack = emptyList(),
            configCatalogPiece = null,
            configValidationError = null,
        )
    }

    fun onConfigResetToStart() {
        val previousBoard = uiState.board
        uiState = uiState.copy(
            board = ChessRules.initialPosition().board,
            configSelectedSquare = null,
            configUndoStack = uiState.configUndoStack + listOf(previousBoard),
            configRedoStack = emptyList(),
            configCatalogPiece = null,
            configValidationError = null,
        )
    }

    fun onConfigToggleSideToMove() {
        uiState = uiState.copy(
            configSideToMove = uiState.configSideToMove.opposite(),
            configValidationError = null,
        )
    }

    /** Re-reads opacity settings (call when overlay becomes visible). */
    fun refreshOpacity() {
        uiState = uiState.copy(
            overlayOpacity = appSettings?.overlayOpacity ?: 1f,
            boardOpacity = appSettings?.boardOpacity ?: 1f,
        )
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
        saveCurrentGame()
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
            gameSaved = false
            startingFen = fen
        } else {
            uiState = uiState.copy(fenLoadError = result.exceptionOrNull()?.message ?: "Invalid FEN")
        }
    }

    fun onFenLoadErrorConsumed() {
        uiState = uiState.copy(fenLoadError = null)
    }

    /**
     * Saves the current game to match history if there are moves.
     */
    fun saveCurrentGame() {
        if (gameSaved) return
        val moves = uiState.moveHistory
        if (moves.isEmpty()) return
        val result = when (uiState.gameStatus) {
            GameStatus.CHECKMATE -> {
                // The side to move is the one who got checkmated
                if (uiState.sideToMove == Side.WHITE) MatchResult.BLACK_WINS else MatchResult.WHITE_WINS
            }
            GameStatus.STALEMATE -> MatchResult.STALEMATE
            else -> MatchResult.INCOMPLETE
        }
        matchHistoryRepository?.save(
            MatchRecord(
                id = UUID.randomUUID().toString(),
                timestampMillis = System.currentTimeMillis(),
                moves = moves,
                result = result,
                assistedSide = uiState.assistedSide,
                startingFen = startingFen,
            ),
        )
        gameSaved = true
    }

    /**
     * Save the game and then execute the close callback.
     */
    fun saveAndClose(close: () -> Unit) {
        saveCurrentGame()
        close()
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


