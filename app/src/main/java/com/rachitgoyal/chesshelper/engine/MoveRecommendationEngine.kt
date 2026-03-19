package com.rachitgoyal.chesshelper.engine

import com.rachitgoyal.chesshelper.domain.chess.model.ChessPosition
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import com.rachitgoyal.chesshelper.engine.model.EngineRecommendation

fun interface MoveRecommendationEngine {
    fun recommend(position: ChessPosition, assistedSide: Side): EngineRecommendation?
}

