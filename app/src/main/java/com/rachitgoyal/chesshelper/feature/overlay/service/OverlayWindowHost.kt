package com.rachitgoyal.chesshelper.feature.overlay.service

import android.content.Context
import android.graphics.Rect
import android.graphics.PixelFormat
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.rachitgoyal.chesshelper.engine.stockfish.StockfishMoveRecommendationEngine
import com.rachitgoyal.chesshelper.R
import com.rachitgoyal.chesshelper.feature.overlay.OverlayBoardViewModel
import com.rachitgoyal.chesshelper.feature.overlay.OverlayWindowContent
import com.rachitgoyal.chesshelper.settings.AppSettings
import com.rachitgoyal.chesshelper.ui.theme.ChessHelperTheme
import kotlin.math.max
import kotlin.math.roundToInt

class OverlayWindowHost(
    context: Context,
    private val onRequestClose: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val overlayViewModel = OverlayBoardViewModel(
        moveRecommendationEngine = StockfishMoveRecommendationEngine(appContext),
        appSettings = AppSettings(appContext),
    )

    private var overlayView: ComposeView? = null
    private var overlayLayoutParams: WindowManager.LayoutParams? = null
    private var composeViewOwner: OverlayComposeViewOwner? = null

    fun show() {
        if (overlayView != null) return

        val owner = OverlayComposeViewOwner().also {
            it.onCreate()
            it.onStart()
            it.onResume()
        }
        composeViewOwner = owner

        val themedContext = ContextThemeWrapper(appContext, R.style.Theme_ChessHelper)
        val composeView = ComposeView(themedContext).apply {
            setViewTreeOwners(owner)
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                reclampWithinScreen()
            }
            setContent {
                ChessHelperTheme {
                    OverlayWindowContent(
                        viewModel = overlayViewModel,
                        onRequestClose = onRequestClose,
                        onDragStart = overlayViewModel::onDragStart,
                        onDrag = ::moveBy,
                        onDragEnd = overlayViewModel::onDragEnd,
                    )
                }
            }
        }

        val params = createLayoutParams()
        overlayView = composeView
        overlayLayoutParams = params
        windowManager.addView(composeView, params)
        OverlayWindowServiceState.isRunning = true
    }

    fun hide() {
        overlayView?.let { windowManager.removeViewImmediate(it) }
        overlayView = null
        overlayLayoutParams = null
        composeViewOwner?.onDestroy()
        composeViewOwner = null
        overlayViewModel.dispose()
        OverlayWindowServiceState.isRunning = false
    }

    fun onConfigurationChanged() {
        reclampWithinScreen()
    }

    private fun moveBy(delta: Offset) {
        val params = overlayLayoutParams ?: return
        val view = overlayView ?: return
        val clampedOffset = OverlayWindowPositioning.clampPosition(
            proposedOffset = androidx.compose.ui.unit.IntOffset(
                x = params.x + delta.x.roundToInt(),
                y = params.y + delta.y.roundToInt(),
            ),
            overlaySize = currentOverlaySizePx(),
            screenSize = currentScreenSizePx(),
            minimumVisibleStripPx = minimumVisibleStripPx(),
        )
        params.x = clampedOffset.x
        params.y = clampedOffset.y
        windowManager.updateViewLayout(view, params)
    }

    private fun reclampWithinScreen() {
        val params = overlayLayoutParams ?: return
        val view = overlayView ?: return
        val clampedOffset = OverlayWindowPositioning.clampPosition(
            proposedOffset = androidx.compose.ui.unit.IntOffset(params.x, params.y),
            overlaySize = currentOverlaySizePx(),
            screenSize = currentScreenSizePx(),
            minimumVisibleStripPx = minimumVisibleStripPx(),
        )
        if (clampedOffset.x == params.x && clampedOffset.y == params.y) return
        params.x = clampedOffset.x
        params.y = clampedOffset.y
        windowManager.updateViewLayout(view, params)
    }

    private fun currentOverlaySizePx(): androidx.compose.ui.unit.IntSize {
        val view = overlayView ?: return androidx.compose.ui.unit.IntSize.Zero
        return androidx.compose.ui.unit.IntSize(
            width = max(view.width, 0),
            height = max(view.height, 0),
        )
    }

    private fun currentScreenSizePx(): androidx.compose.ui.unit.IntSize {
        val bounds: Rect = windowManager.currentWindowMetrics.bounds
        return androidx.compose.ui.unit.IntSize(bounds.width(), bounds.height())
    }

    private fun minimumVisibleStripPx(): Int {
        val density = appContext.resources.displayMetrics.density
        return (48 * density).roundToInt()
    }

    private fun ComposeView.setViewTreeOwners(owner: OverlayComposeViewOwner) {
        setViewTreeLifecycleOwner(owner)
        setViewTreeViewModelStoreOwner(owner)
        setViewTreeSavedStateRegistryOwner(owner)
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 32
            y = 140
            title = OVERLAY_TITLE
        }
    }

    companion object {
        private const val OVERLAY_TITLE = "ChessOverlayAssistant"
    }
}

