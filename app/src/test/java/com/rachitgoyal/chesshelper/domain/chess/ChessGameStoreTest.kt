package com.rachitgoyal.chesshelper.domain.chess

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
}

