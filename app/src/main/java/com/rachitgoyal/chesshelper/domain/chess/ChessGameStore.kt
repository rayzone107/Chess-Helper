package com.rachitgoyal.chesshelper.domain.chess

import com.rachitgoyal.chesshelper.domain.chess.model.ChessPosition
import com.rachitgoyal.chesshelper.domain.chess.model.GameSnapshot
import com.rachitgoyal.chesshelper.domain.chess.model.MoveRecord
import com.rachitgoyal.chesshelper.domain.chess.model.Piece
import com.rachitgoyal.chesshelper.domain.chess.model.Side

class ChessGameStore(
    initialPosition: ChessPosition = ChessRules.initialPosition(),
) {
    private val initialPosition: ChessPosition = initialPosition
    private var position: ChessPosition = initialPosition
    private val previousPositions = mutableListOf<ChessPosition>()
    private val moveHistory = mutableListOf<MoveRecord>()

    private var selectedSquare: String? = null
    private var legalTargets: Set<String> = emptySet()

    fun snapshot(): GameSnapshot {
        val gameStatus = ChessRules.gameStatus(position)
        return GameSnapshot(
            position = position,
            selectedSquare = selectedSquare,
            legalTargets = legalTargets,
            lastMove = moveHistory.lastOrNull(),
            moveHistory = moveHistory.toList(),
            gameStatus = gameStatus,
            checkedKingSquare = ChessRules.checkedKingSquare(position),
        )
    }

    fun currentPosition(): ChessPosition = position

    fun tapSquare(squareId: String): Boolean {
        val square = squareId.lowercase()
        val occupant = position.board[square]
        val activeSelection = selectedSquare

        if (activeSelection != null) {
            val selectedMove = ChessRules.legalMovesFrom(position, activeSelection).firstOrNull { it.to == square }
            if (selectedMove != null) {
                makeMove(selectedMove)
                return true
            }

            when {
                square == activeSelection -> clearSelection()
                occupant?.side == position.sideToMove -> selectSquare(square)
                else -> clearSelection()
            }
            return false
        }

        if (occupant?.side == position.sideToMove) {
            selectSquare(square)
        }
        return false
    }

    fun undo(): Boolean {
        if (previousPositions.isEmpty()) return false
        position = previousPositions.removeAt(previousPositions.lastIndex)
        moveHistory.removeAt(moveHistory.lastIndex)
        clearSelection()
        return true
    }

    fun reset() {
        position = initialPosition
        previousPositions.clear()
        moveHistory.clear()
        clearSelection()
    }

    fun legalMovesForSideToMove(): List<MoveRecord> = ChessRules.legalMoves(position)

    fun applyMove(move: MoveRecord): Boolean {
        val legalMove = ChessRules.legalMoves(position).firstOrNull {
            it.from == move.from &&
                it.to == move.to &&
                it.promotion == move.promotion
        } ?: return false

        makeMove(legalMove)
        return true
    }

    fun loadFen(fen: String): Result<Unit> {
        val result = FenParser.parse(fen)
        result.onSuccess { newPosition ->
            position = newPosition
            previousPositions.clear()
            moveHistory.clear()
            clearSelection()
        }
        return result.map { }
    }

    /**
     * Loads a game from a starting position and replays the given moves,
     * building up the full undo history so each move can be undone.
     */
    fun loadGame(startingPosition: ChessPosition, moves: List<MoveRecord>) {
        position = startingPosition
        previousPositions.clear()
        moveHistory.clear()
        clearSelection()
        for (move in moves) {
            previousPositions += position
            moveHistory += move
            position = ChessRules.applyMove(position, move)
        }
    }

    /**
     * Directly sets the board to the given map of pieces plus side-to-move.
     * Clears all history. Used when exiting config mode.
     */
    fun loadBoard(board: Map<String, Piece>, sideToMove: Side) {
        position = ChessPosition(board = board, sideToMove = sideToMove)
        previousPositions.clear()
        moveHistory.clear()
        clearSelection()
    }

    private fun selectSquare(square: String) {
        selectedSquare = square
        legalTargets = ChessRules.legalMovesFrom(position, square).map { it.to }.toSet()
    }

    private fun clearSelection() {
        selectedSquare = null
        legalTargets = emptySet()
    }

    private fun makeMove(move: MoveRecord) {
        previousPositions += position
        moveHistory += move
        position = ChessRules.applyMove(position, move)
        clearSelection()
    }
}
