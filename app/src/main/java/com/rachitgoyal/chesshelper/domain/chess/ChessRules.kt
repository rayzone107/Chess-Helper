package com.rachitgoyal.chesshelper.domain.chess

import com.rachitgoyal.chesshelper.domain.chess.model.CastlingRights
import com.rachitgoyal.chesshelper.domain.chess.model.ChessPosition
import com.rachitgoyal.chesshelper.domain.chess.model.GameStatus
import com.rachitgoyal.chesshelper.domain.chess.model.MoveRecord
import com.rachitgoyal.chesshelper.domain.chess.model.Piece
import com.rachitgoyal.chesshelper.domain.chess.model.PieceType
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import kotlin.math.abs

object ChessRules {

    fun initialPosition(): ChessPosition {
        val board = mutableMapOf<String, Piece>()
        val order = listOf(
            PieceType.ROOK,
            PieceType.KNIGHT,
            PieceType.BISHOP,
            PieceType.QUEEN,
            PieceType.KING,
            PieceType.BISHOP,
            PieceType.KNIGHT,
            PieceType.ROOK,
        )

        repeat(8) { file ->
            val fileChar = ('a'.code + file).toChar()
            board["${fileChar}2"] = Piece(Side.WHITE, PieceType.PAWN)
            board["${fileChar}7"] = Piece(Side.BLACK, PieceType.PAWN)
            board["${fileChar}1"] = Piece(Side.WHITE, order[file])
            board["${fileChar}8"] = Piece(Side.BLACK, order[file])
        }

        return ChessPosition(board = board, sideToMove = Side.WHITE)
    }

    fun legalMoves(position: ChessPosition): List<MoveRecord> {
        return position.board
            .asSequence()
            .filter { (_, piece) -> piece.side == position.sideToMove }
            .flatMap { (square, _) -> legalMovesFrom(position, square).asSequence() }
            .toList()
    }

    fun legalMovesFrom(position: ChessPosition, from: String): List<MoveRecord> {
        val piece = position.board[from] ?: return emptyList()
        if (piece.side != position.sideToMove) return emptyList()
        return generatePseudoLegalMoves(position, from, piece).filter { move ->
            val applied = applyMove(position, move)
            !isKingInCheck(applied, piece.side)
        }
    }

    fun applyMove(position: ChessPosition, move: MoveRecord): ChessPosition {
        val mutableBoard = position.board.toMutableMap()
        val movingPiece = mutableBoard.remove(move.from) ?: return position
        val normalizedMove = if (move.piece != movingPiece) move.copy(piece = movingPiece) else move

        if (normalizedMove.isEnPassant) {
            val captureSquare = squareName(fileOf(normalizedMove.to), rankOf(normalizedMove.from))
            mutableBoard.remove(captureSquare)
        }

        val movedPiece = if (normalizedMove.promotion != null) {
            Piece(movingPiece.side, normalizedMove.promotion)
        } else {
            movingPiece
        }

        if (normalizedMove.isCastling) {
            when (normalizedMove.to) {
                "g1" -> castleRook(mutableBoard, "h1", "f1")
                "c1" -> castleRook(mutableBoard, "a1", "d1")
                "g8" -> castleRook(mutableBoard, "h8", "f8")
                "c8" -> castleRook(mutableBoard, "a8", "d8")
            }
        }

        mutableBoard.remove(normalizedMove.to)
        mutableBoard[normalizedMove.to] = movedPiece

        val nextRights = updateCastlingRights(
            current = position.castlingRights,
            move = normalizedMove,
            movingPiece = movingPiece,
            capturedPiece = position.board[normalizedMove.to] ?: normalizedMove.capturedPiece,
        )

        val nextEnPassantTarget = if (
            movingPiece.type == PieceType.PAWN && abs(rankOf(normalizedMove.to) - rankOf(normalizedMove.from)) == 2
        ) {
            squareName(fileOf(normalizedMove.from), (rankOf(normalizedMove.from) + rankOf(normalizedMove.to)) / 2)
        } else {
            null
        }

        return ChessPosition(
            board = mutableBoard.toMap(),
            sideToMove = position.sideToMove.opposite(),
            castlingRights = nextRights,
            enPassantTarget = nextEnPassantTarget,
        )
    }

    fun isKingInCheck(position: ChessPosition, side: Side): Boolean {
        val kingSquare = kingSquare(position, side) ?: return false
        return isSquareAttacked(position, kingSquare, side.opposite())
    }

    fun kingSquare(position: ChessPosition, side: Side): String? {
        return position.board.entries.firstOrNull { (_, piece) ->
            piece.side == side && piece.type == PieceType.KING
        }?.key
    }

    fun checkedKingSquare(position: ChessPosition): String? {
        val sideToMove = position.sideToMove
        return kingSquare(position, sideToMove)?.takeIf { isSquareAttacked(position, it, sideToMove.opposite()) }
    }

    fun gameStatus(position: ChessPosition): GameStatus {
        val hasLegalMoves = legalMoves(position).isNotEmpty()
        val inCheck = checkedKingSquare(position) != null
        return when {
            hasLegalMoves && inCheck -> GameStatus.CHECK
            hasLegalMoves -> GameStatus.NORMAL
            inCheck -> GameStatus.CHECKMATE
            else -> GameStatus.STALEMATE
        }
    }

    fun isGameOver(position: ChessPosition): Boolean = gameStatus(position).isTerminal

    private fun generatePseudoLegalMoves(
        position: ChessPosition,
        from: String,
        piece: Piece,
    ): List<MoveRecord> {
        val fromFile = fileOf(from)
        val fromRank = rankOf(from)
        return when (piece.type) {
            PieceType.PAWN -> generatePawnMoves(position, from, fromFile, fromRank, piece)
            PieceType.KNIGHT -> generateKnightMoves(position, from, fromFile, fromRank, piece)
            PieceType.BISHOP -> generateSlidingMoves(position, from, fromFile, fromRank, piece, bishopDirections)
            PieceType.ROOK -> generateSlidingMoves(position, from, fromFile, fromRank, piece, rookDirections)
            PieceType.QUEEN -> generateSlidingMoves(position, from, fromFile, fromRank, piece, queenDirections)
            PieceType.KING -> generateKingMoves(position, from, fromFile, fromRank, piece)
        }
    }

    private fun generatePawnMoves(
        position: ChessPosition,
        from: String,
        fromFile: Int,
        fromRank: Int,
        piece: Piece,
    ): List<MoveRecord> {
        val moves = mutableListOf<MoveRecord>()
        val direction = if (piece.side == Side.WHITE) 1 else -1
        val startRank = if (piece.side == Side.WHITE) 1 else 6
        val promotionRank = if (piece.side == Side.WHITE) 7 else 0

        val oneForwardRank = fromRank + direction
        if (oneForwardRank in 0..7) {
            val oneForward = squareName(fromFile, oneForwardRank)
            if (position.board[oneForward] == null) {
                moves += pawnMove(from, oneForward, piece, promotionRank)
                if (fromRank == startRank) {
                    val twoForwardRank = fromRank + (direction * 2)
                    val twoForward = squareName(fromFile, twoForwardRank)
                    if (position.board[twoForward] == null) {
                        moves += MoveRecord(from = from, to = twoForward, piece = piece)
                    }
                }
            }
        }

        for (fileDelta in listOf(-1, 1)) {
            val targetFile = fromFile + fileDelta
            val targetRank = fromRank + direction
            if (targetFile !in 0..7 || targetRank !in 0..7) continue
            val targetSquare = squareName(targetFile, targetRank)
            val targetPiece = position.board[targetSquare]
            if (targetPiece != null && targetPiece.side != piece.side && targetPiece.type != PieceType.KING) {
                moves += pawnMove(from, targetSquare, piece, promotionRank, capturedPiece = targetPiece)
            } else if (targetSquare == position.enPassantTarget) {
                val captureSquare = squareName(targetFile, fromRank)
                val captured = position.board[captureSquare]
                if (captured != null && captured.side != piece.side && captured.type == PieceType.PAWN) {
                    moves += MoveRecord(
                        from = from,
                        to = targetSquare,
                        piece = piece,
                        capturedPiece = captured,
                        isEnPassant = true,
                    )
                }
            }
        }

        return moves
    }

    private fun pawnMove(
        from: String,
        to: String,
        piece: Piece,
        promotionRank: Int,
        capturedPiece: Piece? = null,
    ): MoveRecord {
        return if (rankOf(to) == promotionRank) {
            MoveRecord(
                from = from,
                to = to,
                piece = piece,
                capturedPiece = capturedPiece,
                promotion = PieceType.QUEEN,
            )
        } else {
            MoveRecord(from = from, to = to, piece = piece, capturedPiece = capturedPiece)
        }
    }

    private fun generateKnightMoves(
        position: ChessPosition,
        from: String,
        fromFile: Int,
        fromRank: Int,
        piece: Piece,
    ): List<MoveRecord> {
        return knightOffsets.mapNotNull { (fileDelta, rankDelta) ->
            val targetFile = fromFile + fileDelta
            val targetRank = fromRank + rankDelta
            toMoveIfValid(position, from, piece, targetFile, targetRank)
        }
    }

    private fun generateSlidingMoves(
        position: ChessPosition,
        from: String,
        fromFile: Int,
        fromRank: Int,
        piece: Piece,
        directions: List<Pair<Int, Int>>,
    ): List<MoveRecord> {
        val moves = mutableListOf<MoveRecord>()
        for ((fileDelta, rankDelta) in directions) {
            var targetFile = fromFile + fileDelta
            var targetRank = fromRank + rankDelta
            while (targetFile in 0..7 && targetRank in 0..7) {
                val square = squareName(targetFile, targetRank)
                val occupant = position.board[square]
                if (occupant == null) {
                    moves += MoveRecord(from = from, to = square, piece = piece)
                } else {
                    if (occupant.side != piece.side && occupant.type != PieceType.KING) {
                        moves += MoveRecord(from = from, to = square, piece = piece, capturedPiece = occupant)
                    }
                    break
                }
                targetFile += fileDelta
                targetRank += rankDelta
            }
        }
        return moves
    }

    private fun generateKingMoves(
        position: ChessPosition,
        from: String,
        fromFile: Int,
        fromRank: Int,
        piece: Piece,
    ): List<MoveRecord> {
        val moves = mutableListOf<MoveRecord>()
        kingOffsets.mapNotNullTo(moves) { (fileDelta, rankDelta) ->
            toMoveIfValid(position, from, piece, fromFile + fileDelta, fromRank + rankDelta)
        }
        moves += generateCastlingMoves(position, from, piece)
        return moves
    }

    private fun generateCastlingMoves(
        position: ChessPosition,
        from: String,
        piece: Piece,
    ): List<MoveRecord> {
        if (piece.type != PieceType.KING || isKingInCheck(position, piece.side)) return emptyList()
        return when (piece.side) {
            Side.WHITE -> buildList {
                if (
                    from == "e1" &&
                    position.castlingRights.whiteKingSide &&
                    position.board["f1"] == null &&
                    position.board["g1"] == null &&
                    position.board["h1"] == Piece(Side.WHITE, PieceType.ROOK) &&
                    !isSquareAttacked(position, "f1", Side.BLACK) &&
                    !isSquareAttacked(position, "g1", Side.BLACK)
                ) {
                    add(MoveRecord(from = "e1", to = "g1", piece = piece, isCastling = true))
                }
                if (
                    from == "e1" &&
                    position.castlingRights.whiteQueenSide &&
                    position.board["d1"] == null &&
                    position.board["c1"] == null &&
                    position.board["b1"] == null &&
                    position.board["a1"] == Piece(Side.WHITE, PieceType.ROOK) &&
                    !isSquareAttacked(position, "d1", Side.BLACK) &&
                    !isSquareAttacked(position, "c1", Side.BLACK)
                ) {
                    add(MoveRecord(from = "e1", to = "c1", piece = piece, isCastling = true))
                }
            }

            Side.BLACK -> buildList {
                if (
                    from == "e8" &&
                    position.castlingRights.blackKingSide &&
                    position.board["f8"] == null &&
                    position.board["g8"] == null &&
                    position.board["h8"] == Piece(Side.BLACK, PieceType.ROOK) &&
                    !isSquareAttacked(position, "f8", Side.WHITE) &&
                    !isSquareAttacked(position, "g8", Side.WHITE)
                ) {
                    add(MoveRecord(from = "e8", to = "g8", piece = piece, isCastling = true))
                }
                if (
                    from == "e8" &&
                    position.castlingRights.blackQueenSide &&
                    position.board["d8"] == null &&
                    position.board["c8"] == null &&
                    position.board["b8"] == null &&
                    position.board["a8"] == Piece(Side.BLACK, PieceType.ROOK) &&
                    !isSquareAttacked(position, "d8", Side.WHITE) &&
                    !isSquareAttacked(position, "c8", Side.WHITE)
                ) {
                    add(MoveRecord(from = "e8", to = "c8", piece = piece, isCastling = true))
                }
            }
        }
    }

    private fun toMoveIfValid(
        position: ChessPosition,
        from: String,
        piece: Piece,
        targetFile: Int,
        targetRank: Int,
    ): MoveRecord? {
        if (targetFile !in 0..7 || targetRank !in 0..7) return null
        val square = squareName(targetFile, targetRank)
        val occupant = position.board[square]
        return when {
            occupant == null -> MoveRecord(from = from, to = square, piece = piece)
            occupant.side != piece.side && occupant.type != PieceType.KING -> {
                MoveRecord(from = from, to = square, piece = piece, capturedPiece = occupant)
            }
            else -> null
        }
    }

    private fun isSquareAttacked(position: ChessPosition, square: String, bySide: Side): Boolean {
        val targetFile = fileOf(square)
        val targetRank = rankOf(square)

        val pawnDirection = if (bySide == Side.WHITE) -1 else 1
        for (fileDelta in listOf(-1, 1)) {
            val attackerFile = targetFile + fileDelta
            val attackerRank = targetRank + pawnDirection
            if (attackerFile !in 0..7 || attackerRank !in 0..7) continue
            val attacker = position.board[squareName(attackerFile, attackerRank)]
            if (attacker?.side == bySide && attacker.type == PieceType.PAWN) {
                return true
            }
        }

        if (knightOffsets.any { (fileDelta, rankDelta) ->
                position.board[squareNameOrNull(targetFile + fileDelta, targetRank + rankDelta)] == Piece(bySide, PieceType.KNIGHT)
            }
        ) {
            return true
        }

        if (isAttackedBySlidingPiece(position, targetFile, targetRank, bySide, bishopDirections, setOf(PieceType.BISHOP, PieceType.QUEEN))) {
            return true
        }

        if (isAttackedBySlidingPiece(position, targetFile, targetRank, bySide, rookDirections, setOf(PieceType.ROOK, PieceType.QUEEN))) {
            return true
        }

        return kingOffsets.any { (fileDelta, rankDelta) ->
            position.board[squareNameOrNull(targetFile + fileDelta, targetRank + rankDelta)] == Piece(bySide, PieceType.KING)
        }
    }

    private fun isAttackedBySlidingPiece(
        position: ChessPosition,
        targetFile: Int,
        targetRank: Int,
        bySide: Side,
        directions: List<Pair<Int, Int>>,
        validTypes: Set<PieceType>,
    ): Boolean {
        for ((fileDelta, rankDelta) in directions) {
            var file = targetFile + fileDelta
            var rank = targetRank + rankDelta
            while (file in 0..7 && rank in 0..7) {
                val occupant = position.board[squareName(file, rank)]
                if (occupant != null) {
                    if (occupant.side == bySide && occupant.type in validTypes) return true
                    break // blocked in this direction, try next
                }
                file += fileDelta
                rank += rankDelta
            }
        }
        return false
    }

    private fun updateCastlingRights(
        current: CastlingRights,
        move: MoveRecord,
        movingPiece: Piece,
        capturedPiece: Piece?,
    ): CastlingRights {
        var whiteKingSide = current.whiteKingSide
        var whiteQueenSide = current.whiteQueenSide
        var blackKingSide = current.blackKingSide
        var blackQueenSide = current.blackQueenSide

        when (movingPiece.side) {
            Side.WHITE -> {
                if (movingPiece.type == PieceType.KING) {
                    whiteKingSide = false
                    whiteQueenSide = false
                }
                if (movingPiece.type == PieceType.ROOK) {
                    if (move.from == "h1") whiteKingSide = false
                    if (move.from == "a1") whiteQueenSide = false
                }
            }

            Side.BLACK -> {
                if (movingPiece.type == PieceType.KING) {
                    blackKingSide = false
                    blackQueenSide = false
                }
                if (movingPiece.type == PieceType.ROOK) {
                    if (move.from == "h8") blackKingSide = false
                    if (move.from == "a8") blackQueenSide = false
                }
            }
        }

        if (capturedPiece?.type == PieceType.ROOK) {
            when (move.to) {
                "h1" -> whiteKingSide = false
                "a1" -> whiteQueenSide = false
                "h8" -> blackKingSide = false
                "a8" -> blackQueenSide = false
            }
        }

        return CastlingRights(
            whiteKingSide = whiteKingSide,
            whiteQueenSide = whiteQueenSide,
            blackKingSide = blackKingSide,
            blackQueenSide = blackQueenSide,
        )
    }

    private fun castleRook(board: MutableMap<String, Piece>, from: String, to: String) {
        val rook = board.remove(from) ?: return
        board[to] = rook
    }

    private fun squareName(file: Int, rank: Int): String {
        return "${('a'.code + file).toChar()}${rank + 1}"
    }

    private fun squareNameOrNull(file: Int, rank: Int): String? {
        if (file !in 0..7 || rank !in 0..7) return null
        return squareName(file, rank)
    }

    private fun fileOf(square: String): Int = square[0] - 'a'

    private fun rankOf(square: String): Int = square[1].digitToInt() - 1

    private val knightOffsets = listOf(
        1 to 2,
        2 to 1,
        2 to -1,
        1 to -2,
        -1 to -2,
        -2 to -1,
        -2 to 1,
        -1 to 2,
    )

    private val kingOffsets = listOf(
        -1 to -1,
        -1 to 0,
        -1 to 1,
        0 to -1,
        0 to 1,
        1 to -1,
        1 to 0,
        1 to 1,
    )

    private val bishopDirections = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
    private val rookDirections = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    private val queenDirections = bishopDirections + rookDirections
}

