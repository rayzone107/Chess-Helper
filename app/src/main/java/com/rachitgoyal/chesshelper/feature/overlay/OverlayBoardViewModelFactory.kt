package com.rachitgoyal.chesshelper.feature.overlay

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rachitgoyal.chesshelper.engine.stockfish.StockfishMoveRecommendationEngine

class OverlayBoardViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OverlayBoardViewModel(
            moveRecommendationEngine = StockfishMoveRecommendationEngine(context),
        ) as T
    }
}

