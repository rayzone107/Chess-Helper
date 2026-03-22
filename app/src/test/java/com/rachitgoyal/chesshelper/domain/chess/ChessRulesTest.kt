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

    @Test
    fun kingMovesDoNotIncludeOpposingKingSquare() {
        val position = position(
            sideToMove = Side.WHITE,
            "e1" to king(Side.WHITE),
            "e2" to king(Side.BLACK),
        )

        val legalTargets = ChessRules.legalMovesFrom(position, "e1").map { it.to }.toSet()

        assertFalse(legalTargets.contains("e2"))
    }

    @Test
    fun legalMovesNeverCaptureTheOpposingKing() {
        val position = position(
            sideToMove = Side.WHITE,
            "e1" to king(Side.WHITE),
            "e7" to rook(Side.WHITE),
            "e8" to king(Side.BLACK),
        )

        val legalTargets = ChessRules.legalMovesFrom(position, "e7").map { it.to }.toSet()

        assertFalse(legalTargets.contains("e8"))
    }

    /**
     * Regression: isAttackedBySlidingPiece must try ALL directions.
     *
     * The queen on g4 attacks e2 along the diagonal g4-f3-e2.
     * The white pawn on d1 occupies the first bishop direction
     * checked from e2 (south-west: e2-d1). With the old bug the
     * pawn caused return-false for the whole sliding-piece check,
     * hiding the queen attack. The king was then allowed to step
     * to e2 which is illegal.
     */
    @Test
    fun kingCannotMoveToSquareAttackedBySlidingPieceWhenOtherDirectionIsBlocked() {
        val position = position(
            sideToMove = Side.WHITE,
            "e1" to king(Side.WHITE),
            "d1" to pawn(Side.WHITE),
            "g4" to queen(Side.BLACK),
            "a8" to king(Side.BLACK),
        )

        val legalTargets = ChessRules.legalMovesFrom(position, "e1").map { it.to }.toSet()

        // e2 is attacked by the black queen via the f3 diagonal - king must not go there
        assertFalse(
            "King should not move to e2 (attacked by queen on g4 via f3)",
            legalTargets.contains("e2"),
        )
        // Sanity: safe squares should still be reachable
        assertTrue("d2 should be a legal king move", legalTargets.contains("d2"))
        assertTrue("f1 should be a legal king move", legalTargets.contains("f1"))
        assertTrue("f2 should be a legal king move", legalTargets.contains("f2"))
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
    private fun pawn(side: Side) = Piece(side, PieceType.PAWN)
}

