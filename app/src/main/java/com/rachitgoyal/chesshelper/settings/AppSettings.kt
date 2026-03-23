package com.rachitgoyal.chesshelper.settings

import android.content.Context
import com.rachitgoyal.chesshelper.domain.chess.model.BoardTheme

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

    var boardTheme: BoardTheme
        get() = BoardTheme.entries.getOrElse(prefs.getInt(KEY_BOARD_THEME, 0)) { BoardTheme.CLASSIC }
        set(value) { prefs.edit().putInt(KEY_BOARD_THEME, value.ordinal).apply() }

    var enableSoundEffects: Boolean
        get() = prefs.getBoolean(KEY_SOUND, false)
        set(value) { prefs.edit().putBoolean(KEY_SOUND, value).apply() }

    /** Overlay-wide opacity (0.2–1.0). Cascades to board. */
    var overlayOpacity: Float
        get() = prefs.getFloat(KEY_OVERLAY_OPACITY, 1f)
        set(value) { prefs.edit().putFloat(KEY_OVERLAY_OPACITY, value.coerceIn(0.2f, 1f)).apply() }

    /** Board-only opacity (0.2–1.0). Multiplied with overlay opacity for the final board alpha. */
    var boardOpacity: Float
        get() = prefs.getFloat(KEY_BOARD_OPACITY, 1f)
        set(value) { prefs.edit().putFloat(KEY_BOARD_OPACITY, value.coerceIn(0.2f, 1f)).apply() }

    companion object {
        private const val PREFS_NAME = "chess_helper_settings"
        private const val KEY_AUTO_APPLY = "auto_apply_best_move"
        private const val KEY_HAPTIC = "enable_haptic_feedback"
        private const val KEY_BOARD_THEME = "board_theme"
        private const val KEY_SOUND = "enable_sound_effects"
        private const val KEY_OVERLAY_OPACITY = "overlay_opacity"
        private const val KEY_BOARD_OPACITY = "board_opacity"
    }
}
