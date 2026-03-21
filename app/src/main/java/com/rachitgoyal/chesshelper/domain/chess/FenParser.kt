package com.rachitgoyal.chesshelper.domain.chess

import com.rachitgoyal.chesshelper.domain.chess.model.CastlingRights
import com.rachitgoyal.chesshelper.domain.chess.model.ChessPosition
import com.rachitgoyal.chesshelper.domain.chess.model.Piece
import com.rachitgoyal.chesshelper.domain.chess.model.PieceType
import com.rachitgoyal.chesshelper.domain.chess.model.Side

object FenParser {
    fun parse(fen: String): Result<ChessPosition> {
        val parts = fen.trim().split(Regex("\\s+"))
        if (parts.size < 4) return Result.failure(IllegalArgumentException("Invalid FEN: need at least 4 fields"))

        val board = mutableMapOf<String, Piece>()
        val ranks = parts[0].split("/")
        if (ranks.size != 8) return Result.failure(IllegalArgumentException("Invalid FEN: expected 8 ranks"))

        ranks.forEachIndexed { rankIdx, rankStr ->
            val rank = 8 - rankIdx
            var file = 0
            for (c in rankStr) {
                when {
                    c.isDigit() -> file += c.digitToInt()
                    else -> {
                        val type = charToPieceType(c.lowercaseChar())
                            ?: return Result.failure(IllegalArgumentException("Invalid piece char: $c"))
                        val side = if (c.isUpperCase()) Side.WHITE else Side.BLACK
                        board["${'a' + file}$rank"] = Piece(side, type)
                        file++
                    }
                }
            }
            if (file != 8) return Result.failure(IllegalArgumentException("Invalid FEN: rank $rank has wrong file count"))
        }

        val sideToMove = when (parts[1]) {
            "w" -> Side.WHITE
            "b" -> Side.BLACK
            else -> return Result.failure(IllegalArgumentException("Invalid side: ${parts[1]}"))
        }

        val cr = parts[2]
        val castlingRights = CastlingRights(
            whiteKingSide = cr.contains('K'),
            whiteQueenSide = cr.contains('Q'),
            blackKingSide = cr.contains('k'),
            blackQueenSide = cr.contains('q'),
        )

        val enPassant = if (parts[3] == "-") null else parts[3]

        return Result.success(
            ChessPosition(
                board = board.toMap(),
                sideToMove = sideToMove,
                castlingRights = castlingRights,
                enPassantTarget = enPassant,
            ),
        )
    }

    private fun charToPieceType(c: Char): PieceType? = when (c) {
        'k' -> PieceType.KING
        'q' -> PieceType.QUEEN
        'r' -> PieceType.ROOK
        'b' -> PieceType.BISHOP
        'n' -> PieceType.KNIGHT
        'p' -> PieceType.PAWN
        else -> null
    }
}

