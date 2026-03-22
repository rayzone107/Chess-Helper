package com.rachitgoyal.chesshelper.domain.chess

import com.rachitgoyal.chesshelper.domain.chess.model.ChessPosition
import com.rachitgoyal.chesshelper.domain.chess.model.GameStatus
import com.rachitgoyal.chesshelper.domain.chess.model.Piece
import com.rachitgoyal.chesshelper.domain.chess.model.PieceType
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessGameStoreTest {

    @Test
    fun selectingAndMovingPawnUpdatesSnapshotAndHighlights() {
        val store = ChessGameStore()

        assertFalse(store.tapSquare("e2"))
        val selectedSnapshot = store.snapshot()
        assertEquals("e2", selectedSnapshot.selectedSquare)
        assertEquals(setOf("e3", "e4"), selectedSnapshot.legalTargets)

        assertTrue(store.tapSquare("e4"))
        val movedSnapshot = store.snapshot()
        assertEquals(Side.BLACK, movedSnapshot.sideToMove)
        assertNull(movedSnapshot.selectedSquare)
        assertTrue(movedSnapshot.legalTargets.isEmpty())
        assertEquals("e2", movedSnapshot.lastMove?.from)
        assertEquals("e4", movedSnapshot.lastMove?.to)
        assertNull(movedSnapshot.board["e2"])
        assertEquals(PieceType.PAWN, movedSnapshot.board["e4"]?.type)
    }

    @Test
    fun undoRestoresPreviousPositionAndHistory() {
        val store = ChessGameStore()

        store.tapSquare("e2")
        store.tapSquare("e4")
        store.tapSquare("e7")
        store.tapSquare("e5")

        assertTrue(store.undo())
        val snapshot = store.snapshot()

        assertEquals(Side.BLACK, snapshot.sideToMove)
        assertEquals(1, snapshot.moveHistory.size)
        assertEquals("e2", snapshot.lastMove?.from)
        assertEquals("e4", snapshot.lastMove?.to)
        assertNull(snapshot.board["e5"])
        assertEquals(PieceType.PAWN, snapshot.board["e7"]?.type)
    }

    @Test
    fun enPassantTargetBecomesLegalAfterDoublePawnAdvance() {
        val store = ChessGameStore()

        store.tapSquare("e2")
        store.tapSquare("e4")
        store.tapSquare("a7")
        store.tapSquare("a6")
        store.tapSquare("e4")
        store.tapSquare("e5")
        store.tapSquare("d7")
        store.tapSquare("d5")

        store.tapSquare("e5")
        val snapshot = store.snapshot()

        assertEquals("e5", snapshot.selectedSquare)
        assertTrue(snapshot.legalTargets.contains("d6"))
    }

    @Test
    fun snapshotSurfacesCheckStatusAndCheckedKingSquare() {
        val store = ChessGameStore(
            initialPosition = position(
                sideToMove = Side.WHITE,
                "e1" to king(Side.WHITE),
                "e8" to rook(Side.BLACK),
                "a8" to king(Side.BLACK),
            ),
        )

        val snapshot = store.snapshot()

        assertEquals(GameStatus.CHECK, snapshot.gameStatus)
        assertEquals("e1", snapshot.checkedKingSquare)
        assertFalse(snapshot.isGameOver)
    }

    @Test
    fun tapSquareRejectsMoveThatWouldLeaveOwnKingInCheck() {
        val store = ChessGameStore(
            initialPosition = position(
                sideToMove = Side.WHITE,
                "e1" to king(Side.WHITE),
                "e2" to rook(Side.WHITE),
                "e8" to rook(Side.BLACK),
                "a8" to king(Side.BLACK),
            ),
        )

        assertFalse(store.tapSquare("e2"))
        assertEquals(setOf("e3", "e4", "e5", "e6", "e7", "e8"), store.snapshot().legalTargets)

        assertFalse(store.tapSquare("d2"))

        val snapshot = store.snapshot()
        assertEquals(Side.WHITE, snapshot.sideToMove)
        assertNull(snapshot.lastMove)
        assertNull(snapshot.board["d2"])
        assertEquals(PieceType.ROOK, snapshot.board["e2"]?.type)
        assertNull(snapshot.selectedSquare)
    }

    @Test
    fun selectingKingDoesNotShowOpposingKingSquareAsLegalTarget() {
        val store = ChessGameStore(
            initialPosition = position(
                sideToMove = Side.WHITE,
                "e1" to king(Side.WHITE),
                "e2" to king(Side.BLACK),
            ),
        )

        assertFalse(store.tapSquare("e1"))

        assertFalse(store.snapshot().legalTargets.contains("e2"))
    }

    private fun position(sideToMove: Side, vararg pieces: Pair<String, Piece>): ChessPosition {
        return ChessPosition(
            board = linkedMapOf(*pieces),
            sideToMove = sideToMove,
        )
    }

    private fun king(side: Side) = Piece(side, PieceType.KING)

    private fun rook(side: Side) = Piece(side, PieceType.ROOK)
}

