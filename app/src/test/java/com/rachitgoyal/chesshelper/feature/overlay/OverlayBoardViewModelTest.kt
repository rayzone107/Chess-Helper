package com.rachitgoyal.chesshelper.feature.overlay

import com.rachitgoyal.chesshelper.domain.chess.ChessGameStore
import com.rachitgoyal.chesshelper.domain.chess.ChessRules
import com.rachitgoyal.chesshelper.domain.chess.model.ChessPosition
import com.rachitgoyal.chesshelper.domain.chess.model.GameStatus
import com.rachitgoyal.chesshelper.domain.chess.model.Piece
import com.rachitgoyal.chesshelper.domain.chess.model.PieceType
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import com.rachitgoyal.chesshelper.engine.EngineUnavailableException
import com.rachitgoyal.chesshelper.engine.MoveRecommendationEngine
import com.rachitgoyal.chesshelper.engine.local.LocalHeuristicMoveRecommendationEngine
import com.rachitgoyal.chesshelper.engine.model.EngineRecommendation
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayBoardViewModelTest {

    @Test
    fun recommendationBecomesAvailableAfterOpponentMoveForBlackPlayer() {
        val viewModel = OverlayBoardViewModel(moveRecommendationEngine = LocalHeuristicMoveRecommendationEngine())

        assertFalse(viewModel.uiState.canRecommend)

        viewModel.onSquareTapped("e2")
        viewModel.onSquareTapped("e4")

        assertEquals(Side.BLACK, viewModel.uiState.sideToMove)
        assertTrue(viewModel.uiState.canRecommend)
    }

    @Test
    fun recommendationIsAvailableImmediatelyWhenPlayingWhite() {
        val viewModel = OverlayBoardViewModel(moveRecommendationEngine = LocalHeuristicMoveRecommendationEngine())

        viewModel.onAssistedSideChanged(Side.WHITE)

        assertEquals(Side.WHITE, viewModel.uiState.assistedSide)
        assertTrue(viewModel.uiState.canRecommend)
    }

    @Test
    fun recommendShowsImmediateLoadingState() {
        val engine = MoveRecommendationEngine { position, _ ->
            Thread.sleep(120)
            EngineRecommendation(
                move = ChessRules.legalMoves(position).first(),
                score = 42,
                summary = "test",
            )
        }
        val viewModel = OverlayBoardViewModel(moveRecommendationEngine = engine)

        viewModel.onAssistedSideChanged(Side.WHITE)
        viewModel.onRecommendClicked()

        assertEquals(RecommendationState.LOADING, viewModel.uiState.recommendationState)
        awaitRecommendation(viewModel)
        assertEquals(RecommendationState.READY, viewModel.uiState.recommendationState)
    }

    @Test
    fun requestingRecommendationPopulatesSummaryAndMarksItStaleAfterNextMove() {
        val viewModel = OverlayBoardViewModel(moveRecommendationEngine = LocalHeuristicMoveRecommendationEngine())

        viewModel.onSquareTapped("e2")
        viewModel.onSquareTapped("e4")
        viewModel.onRecommendClicked()
        awaitRecommendation(viewModel)

        assertEquals(RecommendationState.READY, viewModel.uiState.recommendationState)
        assertNotNull(viewModel.uiState.recommendation)
        assertFalse(viewModel.uiState.isRecommendationStale)

        viewModel.onSquareTapped("e7")
        viewModel.onSquareTapped("e5")

        assertTrue(viewModel.uiState.isRecommendationStale)
        assertEquals(RecommendationState.READY, viewModel.uiState.recommendationState)
    }

    @Test
    fun undoClearsRecommendationAndResetsToIdle() {
        val viewModel = OverlayBoardViewModel(moveRecommendationEngine = LocalHeuristicMoveRecommendationEngine())

        viewModel.onSquareTapped("e2")
        viewModel.onSquareTapped("e4")
        viewModel.onRecommendClicked()
        awaitRecommendation(viewModel)
        viewModel.onUndoClicked()

        assertEquals(RecommendationState.IDLE, viewModel.uiState.recommendationState)
        assertNull(viewModel.uiState.recommendation)
        assertFalse(viewModel.uiState.isRecommendationStale)
        assertEquals(Side.WHITE, viewModel.uiState.sideToMove)
    }

    @Test
    fun applyingRecommendationMakesTheMoveAndClearsPreview() {
        val viewModel = OverlayBoardViewModel(moveRecommendationEngine = LocalHeuristicMoveRecommendationEngine())

        viewModel.onAssistedSideChanged(Side.WHITE)
        viewModel.onRecommendClicked()
        awaitRecommendation(viewModel)

        val recommendedMove = viewModel.uiState.activeRecommendedMove
        assertNotNull(recommendedMove)

        viewModel.onApplyRecommendationClicked()

        assertEquals(RecommendationState.IDLE, viewModel.uiState.recommendationState)
        assertNull(viewModel.uiState.recommendation)
        assertEquals(recommendedMove?.from, viewModel.uiState.lastMove?.from)
        assertEquals(recommendedMove?.to, viewModel.uiState.lastMove?.to)
        assertEquals(Side.BLACK, viewModel.uiState.sideToMove)
    }

    @Test
    fun staleAsyncRecommendationIsDiscardedWhenPlayerSideChanges() {
        val engine = MoveRecommendationEngine { position, _ ->
            Thread.sleep(150)
            EngineRecommendation(
                move = ChessRules.legalMoves(position).first(),
                score = 12,
                summary = "slow",
            )
        }
        val viewModel = OverlayBoardViewModel(moveRecommendationEngine = engine)

        viewModel.onAssistedSideChanged(Side.WHITE)
        viewModel.onRecommendClicked()
        assertEquals(RecommendationState.LOADING, viewModel.uiState.recommendationState)

        viewModel.onAssistedSideChanged(Side.BLACK)
        Thread.sleep(220)

        assertEquals(Side.BLACK, viewModel.uiState.assistedSide)
        assertEquals(RecommendationState.IDLE, viewModel.uiState.recommendationState)
        assertNull(viewModel.uiState.recommendation)
    }

    @Test
    fun duplicateRecommendTapIsIgnoredWhileLoading() {
        val invocationCount = AtomicInteger(0)
        val engine = MoveRecommendationEngine { position, _ ->
            invocationCount.incrementAndGet()
            Thread.sleep(150)
            EngineRecommendation(
                move = ChessRules.legalMoves(position).first(),
                score = 7,
                summary = "counted",
            )
        }
        val viewModel = OverlayBoardViewModel(moveRecommendationEngine = engine)

        viewModel.onAssistedSideChanged(Side.WHITE)
        viewModel.onRecommendClicked()
        viewModel.onRecommendClicked()
        awaitRecommendation(viewModel)

        assertEquals(1, invocationCount.get())
        assertEquals(RecommendationState.READY, viewModel.uiState.recommendationState)
    }

    @Test
    fun staleAsyncRecommendationIsDiscardedWhenBoardPositionChanges() {
        val engine = MoveRecommendationEngine { position, _ ->
            Thread.sleep(150)
            EngineRecommendation(
                move = ChessRules.legalMoves(position).first(),
                score = 9,
                summary = "stale-board",
            )
        }
        val viewModel = OverlayBoardViewModel(moveRecommendationEngine = engine)

        viewModel.onAssistedSideChanged(Side.WHITE)
        viewModel.onRecommendClicked()
        assertEquals(RecommendationState.LOADING, viewModel.uiState.recommendationState)

        viewModel.onSquareTapped("e2")
        viewModel.onSquareTapped("e4")
        Thread.sleep(220)

        assertEquals(Side.BLACK, viewModel.uiState.sideToMove)
        assertEquals(RecommendationState.IDLE, viewModel.uiState.recommendationState)
        assertNull(viewModel.uiState.recommendation)
        assertTrue(viewModel.uiState.lastMove != null)
    }

    @Test
    fun checkStatePropagatesCheckedKingSquareToUiState() {
        val viewModel = OverlayBoardViewModel(
            store = ChessGameStore(
                initialPosition = position(
                    sideToMove = Side.WHITE,
                    "e1" to king(Side.WHITE),
                    "e8" to rook(Side.BLACK),
                    "a8" to king(Side.BLACK),
                ),
            ),
            moveRecommendationEngine = LocalHeuristicMoveRecommendationEngine(),
        )

        viewModel.onAssistedSideChanged(Side.WHITE)

        assertEquals(GameStatus.CHECK, viewModel.uiState.gameStatus)
        assertEquals("e1", viewModel.uiState.checkedKingSquare)
        assertEquals("Check", viewModel.uiState.compactStatusText)
        assertTrue(viewModel.uiState.canRecommend)
    }

    @Test
    fun checkmateDisablesRecommendationAndShowsCompactStatus() {
        val viewModel = OverlayBoardViewModel(
            store = ChessGameStore(
                initialPosition = position(
                    sideToMove = Side.BLACK,
                    "h8" to king(Side.BLACK),
                    "g7" to queen(Side.WHITE),
                    "f6" to king(Side.WHITE),
                ),
            ),
            moveRecommendationEngine = LocalHeuristicMoveRecommendationEngine(),
        )

        assertEquals(GameStatus.CHECKMATE, viewModel.uiState.gameStatus)
        assertEquals("h8", viewModel.uiState.checkedKingSquare)
        assertEquals("Checkmate", viewModel.uiState.compactStatusText)
        assertFalse(viewModel.uiState.canRecommend)
    }

    @Test
    fun stalemateShowsExplicitStatusAndBlocksRecommendation() {
        val viewModel = OverlayBoardViewModel(
            store = ChessGameStore(
                initialPosition = position(
                    sideToMove = Side.BLACK,
                    "h8" to king(Side.BLACK),
                    "g6" to queen(Side.WHITE),
                    "f7" to king(Side.WHITE),
                ),
            ),
            moveRecommendationEngine = LocalHeuristicMoveRecommendationEngine(),
        )

        viewModel.onAssistedSideChanged(Side.BLACK)

        assertEquals(GameStatus.STALEMATE, viewModel.uiState.gameStatus)
        assertNull(viewModel.uiState.checkedKingSquare)
        assertEquals("Stalemate", viewModel.uiState.compactStatusText)
        assertFalse(viewModel.uiState.canRecommend)
    }

    @Test
    fun engineFailureSurfacesExplicitErrorStatusAndDetail() {
        val engine = MoveRecommendationEngine { _, _ ->
            throw EngineUnavailableException("Stockfish stopped responding. Try again.")
        }
        val viewModel = OverlayBoardViewModel(moveRecommendationEngine = engine)

        viewModel.onAssistedSideChanged(Side.WHITE)
        viewModel.onRecommendClicked()
        awaitRecommendation(viewModel)

        assertEquals(RecommendationState.ERROR, viewModel.uiState.recommendationState)
        assertEquals("Engine error", viewModel.uiState.recommendationStatusLabel)
        assertEquals("Engine error", viewModel.uiState.compactStatusText)
        assertEquals("Stockfish stopped responding. Try again.", viewModel.uiState.recommendationBannerText)
        assertTrue(viewModel.uiState.isRecommendationBannerError)
        assertNull(viewModel.uiState.recommendation)
        assertTrue(viewModel.uiState.canRecommend)
    }

    @Test
    fun nullRecommendationUsesNoMoveStatusInsteadOfEngineError() {
        val engine = MoveRecommendationEngine { _, _ -> null }
        val viewModel = OverlayBoardViewModel(moveRecommendationEngine = engine)

        viewModel.onAssistedSideChanged(Side.WHITE)
        viewModel.onRecommendClicked()
        awaitRecommendation(viewModel)

        assertEquals(RecommendationState.ERROR, viewModel.uiState.recommendationState)
        assertEquals("No move", viewModel.uiState.recommendationStatusLabel)
        assertEquals("No move", viewModel.uiState.compactStatusText)
        assertEquals("No legal recommendation available for this position.", viewModel.uiState.recommendationBannerText)
        assertFalse(viewModel.uiState.isRecommendationBannerError)
    }

    private fun awaitRecommendation(viewModel: OverlayBoardViewModel) {
        repeat(40) {
            if (viewModel.uiState.recommendationState != RecommendationState.LOADING) {
                return
            }
            Thread.sleep(25)
        }
        throw AssertionError("Recommendation did not finish loading in time")
    }

    private fun position(sideToMove: Side, vararg pieces: Pair<String, Piece>): ChessPosition {
        return ChessPosition(
            board = linkedMapOf(*pieces),
            sideToMove = sideToMove,
        )
    }

    private fun king(side: Side) = Piece(side, PieceType.KING)

    private fun queen(side: Side) = Piece(side, PieceType.QUEEN)

    private fun rook(side: Side) = Piece(side, PieceType.ROOK)
}

