package com.rachitgoyal.chesshelper.engine.stockfish

import com.rachitgoyal.chesshelper.domain.chess.ChessGameStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StockfishMoveTranslatorTest {

    @Test
    fun mapsSimpleUciMoveToLegalMoveRecord() {
        val position = ChessGameStore().currentPosition()

        val move = StockfishMoveTranslator.legalMoveFromUci(position, "e2e4")

        assertNotNull(move)
        assertEquals("e2", move?.from)
        assertEquals("e4", move?.to)
    }

    @Test
    fun returnsNullForNonLegalUciMove() {
        val position = ChessGameStore().currentPosition()

        val move = StockfishMoveTranslator.legalMoveFromUci(position, "e2e5")

        assertNull(move)
    }
}


