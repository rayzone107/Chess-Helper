package com.rachitgoyal.chesshelper.engine.stockfish

import com.rachitgoyal.chesshelper.domain.chess.ChessGameStore
import com.rachitgoyal.chesshelper.domain.chess.ChessRules
import org.junit.Assert.assertEquals
import org.junit.Test

class ChessPositionFenEncoderTest {

    @Test
    fun encodesInitialPositionToExpectedFen() {
        val fen = ChessPositionFenEncoder.encode(ChessRules.initialPosition())

        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", fen)
    }

    @Test
    fun encodesPositionAfterOpeningMovesIncludingEnPassantTarget() {
        val store = ChessGameStore()
        store.tapSquare("e2")
        store.tapSquare("e4")

        val fen = ChessPositionFenEncoder.encode(store.currentPosition())

        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", fen)
    }
}


