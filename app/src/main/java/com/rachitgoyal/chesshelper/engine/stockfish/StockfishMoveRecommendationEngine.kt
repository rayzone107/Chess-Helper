package com.rachitgoyal.chesshelper.engine.stockfish

import android.content.Context
import android.util.Log
import com.rachitgoyal.chesshelper.domain.chess.ChessRules
import com.rachitgoyal.chesshelper.domain.chess.model.ChessPosition
import com.rachitgoyal.chesshelper.domain.chess.model.MoveRecord
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import com.rachitgoyal.chesshelper.engine.EngineUnavailableException
import com.rachitgoyal.chesshelper.engine.MoveRecommendationEngine
import com.rachitgoyal.chesshelper.engine.model.EngineRecommendation
import java.io.BufferedReader
import java.io.BufferedWriter
import java.util.Locale
import java.util.concurrent.TimeUnit

class StockfishMoveRecommendationEngine(
    context: Context,
    private val moveTimeMs: Int = 5_000,
    private val engineTimeoutMs: Long = 18_000,
    private val hashSizeMb: Int = 128,
    private val threads: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 4),
) : MoveRecommendationEngine, AutoCloseable {

    private companion object {
        private const val TAG = "StockfishEngine"
    }

    private val appContext = context.applicationContext
    private val sessionLock = Any()

    @Volatile
    private var session: EngineSession? = null

    override fun recommend(position: ChessPosition, assistedSide: Side): EngineRecommendation? {
        if (position.sideToMove != assistedSide) return null
        if (ChessRules.legalMoves(position).isEmpty()) return null

        return synchronized(sessionLock) {
            try {
                recommendWithStockfish(position)
            } catch (exception: EngineUnavailableException) {
                discardSession()
                throw exception
            } catch (exception: Exception) {
                discardSession()
                throw EngineUnavailableException(
                    message = "Stockfish analysis failed. Try again.",
                    cause = exception,
                )
            }
        }
    }

    override fun close() {
        synchronized(sessionLock) {
            discardSession()
        }
    }

    private fun recommendWithStockfish(position: ChessPosition): EngineRecommendation {
        val activeSession = getOrCreateSession()

        awaitReady(activeSession)
        send(activeSession.writer, "position fen ${ChessPositionFenEncoder.encode(position)}")
        send(activeSession.writer, "go movetime $moveTimeMs")

        var bestMoveUci: String? = null
        var bestScore = 0
        var bestDepth = 0
        var mateScore: Int? = null

        awaitLine(activeSession.process, activeSession.reader, engineTimeoutMs) { line ->
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
        } ?: throw EngineUnavailableException("Stockfish timed out while analyzing this position.")

        val moveUci = bestMoveUci?.takeUnless { it == "(none)" }
            ?: throw EngineUnavailableException("Stockfish did not return a best move.")

        val move = StockfishMoveTranslator.legalMoveFromUci(position, moveUci)
            ?: throw EngineUnavailableException("Stockfish returned a move the app could not validate.")

        val score = mateScore?.let { mate -> if (mate > 0) 100_000 - mate else -100_000 - mate } ?: bestScore
        val summary = buildSummary(move, bestDepth, bestScore, mateScore)

        return EngineRecommendation(
            move = move,
            score = score,
            summary = summary,
        )
    }

    private fun getOrCreateSession(): EngineSession {
        val existingSession = session
        if (existingSession != null && existingSession.process.isAlive) {
            return existingSession
        }

        discardSession()

        val engineBinary = try {
            StockfishAssetInstaller.ensureInstalled(appContext)
        } catch (exception: Exception) {
            Log.e(TAG, "Stockfish installation failed: ${exception.javaClass.simpleName}: ${exception.message}", exception)
            throw EngineUnavailableException(
                "Stockfish could not be installed: ${exception.message ?: exception.javaClass.simpleName}",
                exception,
            )
        }

        val process = try {
            ProcessBuilder(engineBinary.absolutePath)
                .directory(engineBinary.parentFile)
                .redirectErrorStream(true)
                .start()
        } catch (exception: Exception) {
            throw EngineUnavailableException(
                exception.message?.let { "Stockfish could not be started on this device. $it" }
                    ?: "Stockfish could not be started on this device.",
                exception,
            )
        }

        val newSession = EngineSession(
            process = process,
            reader = process.inputStream.bufferedReader(),
            writer = process.outputStream.bufferedWriter(),
        )

        try {
            send(newSession.writer, "uci")
            awaitLine(newSession.process, newSession.reader, engineTimeoutMs) { it == "uciok" }
                ?: throw EngineUnavailableException("Stockfish did not finish the UCI handshake.")

            send(newSession.writer, "setoption name Threads value ${threads.coerceAtLeast(1)}")
            send(newSession.writer, "setoption name Hash value ${hashSizeMb.coerceAtLeast(1)}")
            send(newSession.writer, "setoption name UCI_AnalyseMode value true")
            send(newSession.writer, "setoption name MultiPV value 1")
            awaitReady(newSession)
        } catch (exception: Exception) {
            newSession.closeNow()
            if (exception is EngineUnavailableException) throw exception
            throw EngineUnavailableException("Stockfish initialization failed.", exception)
        }

        session = newSession
        return newSession
    }

    private fun discardSession() {
        session?.closeNow()
        session = null
    }

    private fun awaitReady(session: EngineSession) {
        send(session.writer, "isready")
        awaitLine(session.process, session.reader, engineTimeoutMs) { it == "readyok" }
            ?: throw EngineUnavailableException("Stockfish did not become ready.")
    }

    private fun send(writer: BufferedWriter, command: String) {
        try {
            writer.write(command)
            writer.newLine()
            writer.flush()
        } catch (exception: Exception) {
            throw EngineUnavailableException("Failed to talk to Stockfish.", exception)
        }
    }

    private fun awaitLine(
        process: Process,
        reader: BufferedReader,
        timeoutMs: Long,
        predicate: (String) -> Boolean,
    ): String? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                while (reader.ready()) {
                    val line = reader.readLine() ?: return null
                    if (predicate(line)) return line
                }
                if (!process.isAlive && !reader.ready()) return null
                Thread.sleep(10)
            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()
                throw EngineUnavailableException("Stockfish analysis was interrupted.", exception)
            } catch (exception: Exception) {
                throw EngineUnavailableException("Stockfish stopped responding.", exception)
            }
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
            else -> "eval ${String.format(Locale.US, "%+.2f", scoreCp / 100.0)}"
        }
        return "${move.notation} • d$depth • $evalText"
    }

    private data class ParsedInfo(
        val depth: Int?,
        val scoreCp: Int?,
        val scoreMate: Int?,
    )

    private data class EngineSession(
        val process: Process,
        val reader: BufferedReader,
        val writer: BufferedWriter,
    ) {
        fun closeNow() {
            runCatching {
                writer.write("quit")
                writer.newLine()
                writer.flush()
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
}

