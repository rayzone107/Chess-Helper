package com.rachitgoyal.chesshelper.engine.model

import com.rachitgoyal.chesshelper.domain.chess.model.MoveRecord

data class EngineRecommendation(
    val move: MoveRecord,
    val score: Int,
    val summary: String,
)

