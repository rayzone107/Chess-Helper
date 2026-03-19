package com.rachitgoyal.chesshelper.engine.stockfish

import com.rachitgoyal.chesshelper.domain.chess.ChessRules
import com.rachitgoyal.chesshelper.domain.chess.model.ChessPosition
import com.rachitgoyal.chesshelper.domain.chess.model.MoveRecord
import com.rachitgoyal.chesshelper.domain.chess.model.PieceType

internal object StockfishMoveTranslator {
    fun legalMoveFromUci(position: ChessPosition, uci: String): MoveRecord? {
        val from = uci.take(2)
        val to = uci.drop(2).take(2)
        val promotion = uci.getOrNull(4)?.let { promotionPieceType(it) }

        return ChessRules.legalMoves(position).firstOrNull { move ->
            move.from == from && move.to == to && move.promotion == promotion
        }
    }

    private fun promotionPieceType(symbol: Char): PieceType = when (symbol.lowercaseChar()) {
        'q' -> PieceType.QUEEN
        'r' -> PieceType.ROOK
        'b' -> PieceType.BISHOP
        'n' -> PieceType.KNIGHT
        else -> PieceType.QUEEN
    }
}

