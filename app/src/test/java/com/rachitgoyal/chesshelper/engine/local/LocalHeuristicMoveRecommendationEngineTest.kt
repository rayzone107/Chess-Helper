package com.rachitgoyal.chesshelper.engine.local

import com.rachitgoyal.chesshelper.domain.chess.ChessGameStore
import com.rachitgoyal.chesshelper.domain.chess.ChessRules
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalHeuristicMoveRecommendationEngineTest {

    @Test
    fun returnsNullWhenItIsNotTheRequestedSideTurn() {
        val engine = LocalHeuristicMoveRecommendationEngine()
        val position = ChessRules.initialPosition()

        val recommendation = engine.recommend(position, Side.BLACK)

        assertNull(recommendation)
    }

    @Test
    fun recommendsALegalMoveForTheActiveStudySide() {
        val store = ChessGameStore()
        store.tapSquare("e2")
        store.tapSquare("e4")

        val engine = LocalHeuristicMoveRecommendationEngine()
        val position = store.currentPosition()
        val legalMoves = ChessRules.legalMoves(position)

        val recommendation = engine.recommend(position, Side.BLACK)

        assertNotNull(recommendation)
        assertTrue(legalMoves.any { it.from == recommendation?.move?.from && it.to == recommendation.move.to })
    }
}

