package com.rachitgoyal.chesshelper.engine.local

import com.rachitgoyal.chesshelper.domain.chess.ChessRules
import com.rachitgoyal.chesshelper.domain.chess.model.ChessPosition
import com.rachitgoyal.chesshelper.domain.chess.model.PieceType
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import com.rachitgoyal.chesshelper.engine.MoveRecommendationEngine
import com.rachitgoyal.chesshelper.engine.model.EngineRecommendation

class LocalHeuristicMoveRecommendationEngine(
    private val searchDepth: Int = 2,
) : MoveRecommendationEngine {

    override fun recommend(position: ChessPosition, assistedSide: Side): EngineRecommendation? {
        if (position.sideToMove != assistedSide) return null
        val legalMoves = ChessRules.legalMoves(position)
        if (legalMoves.isEmpty()) return null

        val bestMove = legalMoves.maxByOrNull { move ->
            val applied = ChessRules.applyMove(position, move)
            minimax(applied, searchDepth - 1, assistedSide)
        } ?: return null

        val score = minimax(ChessRules.applyMove(position, bestMove), searchDepth - 1, assistedSide)
        val summary = buildString {
            append(bestMove.notation)
            append(" • eval ")
            append(String.format("%+.2f", score / 100.0))
        }

        return EngineRecommendation(
            move = bestMove,
            score = score,
            summary = summary,
        )
    }

    private fun minimax(position: ChessPosition, depth: Int, perspective: Side): Int {
        val legalMoves = ChessRules.legalMoves(position)
        if (depth == 0 || legalMoves.isEmpty()) {
            return evaluate(position, perspective, legalMoves.isEmpty())
        }

        val childScores = legalMoves.map { move ->
            minimax(ChessRules.applyMove(position, move), depth - 1, perspective)
        }

        return if (position.sideToMove == perspective) {
            childScores.maxOrNull() ?: evaluate(position, perspective, false)
        } else {
            childScores.minOrNull() ?: evaluate(position, perspective, false)
        }
    }

    private fun evaluate(position: ChessPosition, perspective: Side, noMovesAvailable: Boolean): Int {
        if (noMovesAvailable) {
            return when {
                ChessRules.isKingInCheck(position, position.sideToMove) && position.sideToMove == perspective -> -100_000
                ChessRules.isKingInCheck(position, position.sideToMove) -> 100_000
                else -> 0
            }
        }

        var materialScore = 0
        var positionalScore = 0
        position.board.forEach { (square, piece) ->
            val signed = if (piece.side == perspective) 1 else -1
            materialScore += piece.type.materialValue * signed
            positionalScore += squareBonus(square, piece.side, piece.type) * signed
        }

        val mobilityScore = ChessRules.legalMoves(position).size * if (position.sideToMove == perspective) 4 else -4
        return materialScore + positionalScore + mobilityScore
    }

    private fun squareBonus(square: String, side: Side, type: PieceType): Int {
        val file = square[0] - 'a'
        val rank = square[1].digitToInt() - 1
        val normalizedRank = if (side == Side.WHITE) rank else 7 - rank
        val centerDistance = kotlin.math.abs(file - 3.5) + kotlin.math.abs(rank - 3.5)
        val centerBonus = ((6 - centerDistance) * 4).toInt()
        val advancementBonus = when (type) {
            PieceType.PAWN -> normalizedRank * 6
            PieceType.KNIGHT, PieceType.BISHOP -> centerBonus
            PieceType.QUEEN -> centerBonus / 2
            PieceType.ROOK -> normalizedRank * 2
            PieceType.KING -> -centerBonus
        }
        return advancementBonus
    }
}

