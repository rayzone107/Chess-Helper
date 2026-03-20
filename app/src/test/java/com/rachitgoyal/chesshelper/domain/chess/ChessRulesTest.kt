package com.rachitgoyal.chesshelper.domain.chess
import com.rachitgoyal.chesshelper.domain.chess.model.ChessPosition
import com.rachitgoyal.chesshelper.domain.chess.model.GameStatus
import com.rachitgoyal.chesshelper.domain.chess.model.Piece
import com.rachitgoyal.chesshelper.domain.chess.model.PieceType
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class ChessRulesTest {
    @Test
    fun gameStatusDetectsCheckAndCheckedKingSquare() {
        val position = position(
            sideToMove = Side.WHITE,
            "e1" to king(Side.WHITE),
            "e8" to rook(Side.BLACK),
            "a8" to king(Side.BLACK),
        )
        assertEquals(GameStatus.CHECK, ChessRules.gameStatus(position))
        assertEquals("e1", ChessRules.checkedKingSquare(position))
        assertFalse(ChessRules.isGameOver(position))
    }
    @Test
    fun legalMovesFilterOutMovesThatExposeOwnKing() {
        val position = position(
            sideToMove = Side.WHITE,
            "e1" to king(Side.WHITE),
            "e2" to rook(Side.WHITE),
            "e8" to rook(Side.BLACK),
            "a8" to king(Side.BLACK),
        )
        val legalTargets = ChessRules.legalMovesFrom(position, "e2").map { it.to }.toSet()
        assertEquals(setOf("e3", "e4", "e5", "e6", "e7", "e8"), legalTargets)
        assertFalse(legalTargets.contains("d2"))
        assertFalse(legalTargets.contains("f2"))
    }
    @Test
    fun gameStatusDistinguishesCheckmateAndStalemate() {
        val checkmate = position(
            sideToMove = Side.BLACK,
            "h8" to king(Side.BLACK),
            "g7" to queen(Side.WHITE),
            "f6" to king(Side.WHITE),
        )
        val stalemate = position(
            sideToMove = Side.BLACK,
            "h8" to king(Side.BLACK),
            "g6" to queen(Side.WHITE),
            "f7" to king(Side.WHITE),
        )
        assertEquals(GameStatus.CHECKMATE, ChessRules.gameStatus(checkmate))
        assertEquals("h8", ChessRules.checkedKingSquare(checkmate))
        assertTrue(ChessRules.isGameOver(checkmate))
        assertEquals(GameStatus.STALEMATE, ChessRules.gameStatus(stalemate))
        assertEquals(null, ChessRules.checkedKingSquare(stalemate))
        assertTrue(ChessRules.isGameOver(stalemate))
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
