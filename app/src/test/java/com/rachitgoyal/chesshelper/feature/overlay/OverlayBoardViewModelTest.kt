package com.rachitgoyal.chesshelper.feature.overlay

import com.rachitgoyal.chesshelper.domain.chess.ChessRules
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import com.rachitgoyal.chesshelper.engine.MoveRecommendationEngine
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
        val viewModel = OverlayBoardViewModel()

        assertFalse(viewModel.uiState.canRecommend)

        viewModel.onSquareTapped("e2")
        viewModel.onSquareTapped("e4")

        assertEquals(Side.BLACK, viewModel.uiState.sideToMove)
        assertTrue(viewModel.uiState.canRecommend)
    }

    @Test
    fun recommendationIsAvailableImmediatelyWhenPlayingWhite() {
        val viewModel = OverlayBoardViewModel()

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
        val viewModel = OverlayBoardViewModel()

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
        val viewModel = OverlayBoardViewModel()

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
        val viewModel = OverlayBoardViewModel()

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

    private fun awaitRecommendation(viewModel: OverlayBoardViewModel) {
        repeat(40) {
            if (viewModel.uiState.recommendationState != RecommendationState.LOADING) {
                return
            }
            Thread.sleep(25)
        }
        throw AssertionError("Recommendation did not finish loading in time")
    }
}

