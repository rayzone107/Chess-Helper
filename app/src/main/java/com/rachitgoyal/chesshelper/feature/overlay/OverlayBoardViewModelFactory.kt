package com.rachitgoyal.chesshelper.feature.overlay

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rachitgoyal.chesshelper.engine.stockfish.StockfishMoveRecommendationEngine
import com.rachitgoyal.chesshelper.settings.AppSettings

class OverlayBoardViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OverlayBoardViewModel(
            moveRecommendationEngine = StockfishMoveRecommendationEngine(context),
            appSettings = AppSettings(context),
        ) as T
    }
}
