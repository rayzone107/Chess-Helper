package com.rachitgoyal.chesshelper.settings

import android.content.Context

/**
 * Persists user preferences for Chess Helper using SharedPreferences.
 * All properties are read/written directly — no caching — so multiple
 * instances sharing the same [Context] are always in sync.
 */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * When true (default), the best move returned by Stockfish is applied
     * to the board automatically — no need to tap "Play suggested move".
     */
    var autoApplyBestMove: Boolean
        get() = prefs.getBoolean(KEY_AUTO_APPLY, true)
        set(value) { prefs.edit().putBoolean(KEY_AUTO_APPLY, value).apply() }

    /**
     * When true (default), the device vibrates briefly on legal moves in the overlay.
     */
    var enableHapticFeedback: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC, true)
        set(value) { prefs.edit().putBoolean(KEY_HAPTIC, value).apply() }

    companion object {
        private const val PREFS_NAME = "chess_helper_settings"
        private const val KEY_AUTO_APPLY = "auto_apply_best_move"
        private const val KEY_HAPTIC = "enable_haptic_feedback"
    }
}
