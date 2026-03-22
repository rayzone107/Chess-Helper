package com.rachitgoyal.chesshelper.data

import android.content.Context
import com.rachitgoyal.chesshelper.domain.chess.model.MatchRecord
import com.rachitgoyal.chesshelper.domain.chess.model.MatchResult
import com.rachitgoyal.chesshelper.domain.chess.model.MoveRecord
import com.rachitgoyal.chesshelper.domain.chess.model.Piece
import com.rachitgoyal.chesshelper.domain.chess.model.PieceType
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists match history to SharedPreferences using org.json serialization.
 * FIFO eviction keeps at most [MAX_MATCHES] entries.
 */
class MatchHistoryRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(match: MatchRecord) {
        val all = loadAll().toMutableList()
        all.add(0, match) // newest first
        if (all.size > MAX_MATCHES) {
            all.subList(MAX_MATCHES, all.size).clear()
        }
        prefs.edit().putString(KEY_MATCHES, encodeList(all).toString()).apply()
    }

    fun loadAll(): List<MatchRecord> {
        val raw = prefs.getString(KEY_MATCHES, null) ?: return emptyList()
        return try {
            decodeList(JSONArray(raw))
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getById(id: String): MatchRecord? = loadAll().firstOrNull { it.id == id }

    fun deleteAll() {
        prefs.edit().remove(KEY_MATCHES).apply()
    }

    // --- JSON serialization ---

    private fun encodeList(matches: List<MatchRecord>): JSONArray {
        return JSONArray().apply {
            matches.forEach { put(encode(it)) }
        }
    }

    private fun decodeList(array: JSONArray): List<MatchRecord> {
        return (0 until array.length()).mapNotNull { i ->
            try {
                decode(array.getJSONObject(i))
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun encode(match: MatchRecord): JSONObject = JSONObject().apply {
        put("id", match.id)
        put("ts", match.timestampMillis)
        put("result", match.result.name)
        put("side", match.assistedSide.name)
        match.startingFen?.let { put("fen", it) }
        put("moves", JSONArray().apply {
            match.moves.forEach { put(encodeMove(it)) }
        })
    }

    private fun decode(obj: JSONObject): MatchRecord {
        val movesArray = obj.getJSONArray("moves")
        val moves = (0 until movesArray.length()).map { i ->
            decodeMove(movesArray.getJSONObject(i))
        }
        return MatchRecord(
            id = obj.getString("id"),
            timestampMillis = obj.getLong("ts"),
            result = MatchResult.valueOf(obj.getString("result")),
            assistedSide = Side.valueOf(obj.getString("side")),
            moves = moves,
            startingFen = obj.optString("fen", null),
        )
    }

    private fun encodeMove(move: MoveRecord): JSONObject = JSONObject().apply {
        put("from", move.from)
        put("to", move.to)
        put("piece", encodePiece(move.piece))
        move.capturedPiece?.let { put("captured", encodePiece(it)) }
        move.promotion?.let { put("promo", it.name) }
        if (move.isCastling) put("castle", true)
        if (move.isEnPassant) put("ep", true)
    }

    private fun decodeMove(obj: JSONObject): MoveRecord = MoveRecord(
        from = obj.getString("from"),
        to = obj.getString("to"),
        piece = decodePiece(obj.getJSONObject("piece")),
        capturedPiece = if (obj.has("captured")) decodePiece(obj.getJSONObject("captured")) else null,
        promotion = if (obj.has("promo")) PieceType.valueOf(obj.getString("promo")) else null,
        isCastling = obj.optBoolean("castle", false),
        isEnPassant = obj.optBoolean("ep", false),
    )

    private fun encodePiece(piece: Piece): JSONObject = JSONObject().apply {
        put("side", piece.side.name)
        put("type", piece.type.name)
    }

    private fun decodePiece(obj: JSONObject): Piece = Piece(
        side = Side.valueOf(obj.getString("side")),
        type = PieceType.valueOf(obj.getString("type")),
    )

    companion object {
        private const val PREFS_NAME = "chess_match_history"
        private const val KEY_MATCHES = "matches"
        private const val MAX_MATCHES = 50
    }
}

