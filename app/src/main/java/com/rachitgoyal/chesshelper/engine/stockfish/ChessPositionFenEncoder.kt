package com.rachitgoyal.chesshelper.engine.stockfish

import com.rachitgoyal.chesshelper.domain.chess.model.ChessPosition
import com.rachitgoyal.chesshelper.domain.chess.model.Piece
import com.rachitgoyal.chesshelper.domain.chess.model.PieceType
import com.rachitgoyal.chesshelper.domain.chess.model.Side

internal object ChessPositionFenEncoder {
    fun encode(position: ChessPosition): String {
        val placement = buildString {
            for (rank in 8 downTo 1) {
                var emptyCount = 0
                for (file in 'a'..'h') {
                    val piece = position.board["$file$rank"]
                    if (piece == null) {
                        emptyCount += 1
                    } else {
                        if (emptyCount > 0) {
                            append(emptyCount)
                            emptyCount = 0
                        }
                        append(piece.toFenChar())
                    }
                }
                if (emptyCount > 0) append(emptyCount)
                if (rank > 1) append('/')
            }
        }

        val sideToMove = if (position.sideToMove == Side.WHITE) "w" else "b"
        val castling = buildString {
            if (position.castlingRights.whiteKingSide) append('K')
            if (position.castlingRights.whiteQueenSide) append('Q')
            if (position.castlingRights.blackKingSide) append('k')
            if (position.castlingRights.blackQueenSide) append('q')
        }.ifEmpty { "-" }
        val enPassant = position.enPassantTarget ?: "-"

        return "$placement $sideToMove $castling $enPassant 0 1"
    }

    private fun Piece.toFenChar(): Char {
        val base = when (type) {
            PieceType.KING -> 'k'
            PieceType.QUEEN -> 'q'
            PieceType.ROOK -> 'r'
            PieceType.BISHOP -> 'b'
            PieceType.KNIGHT -> 'n'
            PieceType.PAWN -> 'p'
        }
        return if (side == Side.WHITE) base.uppercaseChar() else base
    }
}

