package com.rachitgoyal.chesshelper.engine.stockfish

import android.content.Context
import com.rachitgoyal.chesshelper.domain.chess.model.ChessPosition
import com.rachitgoyal.chesshelper.domain.chess.model.MoveRecord
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import com.rachitgoyal.chesshelper.engine.MoveRecommendationEngine
import com.rachitgoyal.chesshelper.engine.local.LocalHeuristicMoveRecommendationEngine
import com.rachitgoyal.chesshelper.engine.model.EngineRecommendation
import java.io.BufferedReader
import java.io.BufferedWriter
import java.util.concurrent.TimeUnit

class StockfishMoveRecommendationEngine(
    context: Context,
    private val fallback: MoveRecommendationEngine = LocalHeuristicMoveRecommendationEngine(),
    private val moveTimeMs: Int = 1200,
    private val engineTimeoutMs: Long = 7_500,
    private val hashSizeMb: Int = 16,
    private val threads: Int = 2,
) : MoveRecommendationEngine {

    private val appContext = context.applicationContext

    override fun recommend(position: ChessPosition, assistedSide: Side): EngineRecommendation? {
        if (position.sideToMove != assistedSide) return null

        return try {
            recommendWithStockfish(position, assistedSide) ?: fallback.recommend(position, assistedSide)
        } catch (_: Exception) {
            fallback.recommend(position, assistedSide)
        }
    }

    private fun recommendWithStockfish(position: ChessPosition, assistedSide: Side): EngineRecommendation? {
        val engineBinary = StockfishAssetInstaller.ensureInstalled(appContext)
        val process = ProcessBuilder(engineBinary.absolutePath)
            .directory(engineBinary.parentFile)
            .redirectErrorStream(true)
            .start()

        val reader = process.inputStream.bufferedReader()
        val writer = process.outputStream.bufferedWriter()

        return try {
            send(writer, "uci")
            awaitLine(process, reader, engineTimeoutMs) { it == "uciok" } ?: return null

            send(writer, "setoption name Threads value ${threads.coerceAtLeast(1)}")
            send(writer, "setoption name Hash value ${hashSizeMb.coerceAtLeast(1)}")
            send(writer, "isready")
            awaitLine(process, reader, engineTimeoutMs) { it == "readyok" } ?: return null

            send(writer, "ucinewgame")
            send(writer, "position fen ${ChessPositionFenEncoder.encode(position)}")
            send(writer, "go movetime $moveTimeMs")

            var bestMoveUci: String? = null
            var bestScore = 0
            var bestDepth = 0
            var mateScore: Int? = null

            awaitLine(process, reader, engineTimeoutMs) { line ->
                when {
                    line.startsWith("info ") -> {
                        parseScoreInfo(line)?.let { info ->
                            bestDepth = info.depth ?: bestDepth
                            bestScore = info.scoreCp ?: bestScore
                            mateScore = info.scoreMate ?: mateScore
                        }
                        false
                    }

                    line.startsWith("bestmove ") -> {
                        bestMoveUci = line.substringAfter("bestmove ").substringBefore(' ').trim()
                        true
                    }

                    else -> false
                }
            } ?: return null

            val move = bestMoveUci
                ?.takeUnless { it == "(none)" }
                ?.let { uci -> StockfishMoveTranslator.legalMoveFromUci(position, uci) }
                ?: return null

            val score = mateScore?.let { mate -> if (mate > 0) 100_000 - mate else -100_000 - mate } ?: bestScore
            val summary = buildSummary(move, bestDepth, bestScore, mateScore)

            EngineRecommendation(
                move = move,
                score = score,
                summary = summary,
            )
        } finally {
            try {
                send(writer, "quit")
            } catch (_: Exception) {
                // Ignore shutdown failures.
            }
            runCatching { writer.close() }
            runCatching { reader.close() }
            process.destroy()
            if (process.isAlive) {
                process.waitFor(200, TimeUnit.MILLISECONDS)
                if (process.isAlive) process.destroyForcibly()
            }
        }
    }

    private fun send(writer: BufferedWriter, command: String) {
        writer.write(command)
        writer.newLine()
        writer.flush()
    }

    private fun awaitLine(
        process: Process,
        reader: BufferedReader,
        timeoutMs: Long,
        predicate: (String) -> Boolean,
    ): String? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            while (reader.ready()) {
                val line = reader.readLine() ?: return null
                if (predicate(line)) return line
            }
            if (!process.isAlive && !reader.ready()) return null
            Thread.sleep(10)
        }
        return null
    }


    private fun parseScoreInfo(line: String): ParsedInfo? {
        val tokens = line.split(' ')
        var depth: Int? = null
        var scoreCp: Int? = null
        var scoreMate: Int? = null

        var index = 0
        while (index < tokens.size) {
            when (tokens[index]) {
                "depth" -> depth = tokens.getOrNull(index + 1)?.toIntOrNull()
                "score" -> when (tokens.getOrNull(index + 1)) {
                    "cp" -> scoreCp = tokens.getOrNull(index + 2)?.toIntOrNull()
                    "mate" -> scoreMate = tokens.getOrNull(index + 2)?.toIntOrNull()
                }
            }
            index += 1
        }

        return if (depth != null || scoreCp != null || scoreMate != null) {
            ParsedInfo(depth = depth, scoreCp = scoreCp, scoreMate = scoreMate)
        } else {
            null
        }
    }

    private fun buildSummary(
        move: MoveRecord,
        depth: Int,
        scoreCp: Int,
        scoreMate: Int?,
    ): String {
        val evalText = when {
            scoreMate != null -> "mate ${if (scoreMate > 0) "+" else ""}$scoreMate"
            else -> "eval ${String.format("%+.2f", scoreCp / 100.0)}"
        }
        return "${move.notation} • d$depth • $evalText"
    }

    private data class ParsedInfo(
        val depth: Int?,
        val scoreCp: Int?,
        val scoreMate: Int?,
    )
}

