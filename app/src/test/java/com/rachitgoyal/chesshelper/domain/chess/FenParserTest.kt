package com.rachitgoyal.chesshelper.domain.chess

import com.rachitgoyal.chesshelper.domain.chess.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FenParserTest {

    @Test
    fun startingPositionParsesCorrectly() {
        val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val pos = FenParser.parse(fen).getOrThrow()
        assertEquals(Side.WHITE, pos.sideToMove)
        assertEquals(32, pos.board.size)
        assertTrue(pos.castlingRights.whiteKingSide)
        assertTrue(pos.castlingRights.blackQueenSide)
        assertNull(pos.enPassantTarget)
    }

    @Test
    fun midGamePositionParsesCorrectly() {
        val fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2"
        val pos = FenParser.parse(fen).getOrThrow()
        assertEquals(Side.WHITE, pos.sideToMove)
        assertEquals("e6", pos.enPassantTarget)
        assertNotNull(pos.board["e5"])
        assertNotNull(pos.board["e4"])
    }

    @Test
    fun invalidFenReturnsFailure() {
        val result = FenParser.parse("not a fen string")
        assertTrue(result.isFailure)
    }

    @Test
    fun fenWithNoCastlingRightsParsesCorrectly() {
        val fen = "8/8/8/8/8/8/8/4K2k w - - 0 1"
        val pos = FenParser.parse(fen).getOrThrow()
        assertFalse(pos.castlingRights.whiteKingSide)
        assertFalse(pos.castlingRights.blackQueenSide)
    }
}

