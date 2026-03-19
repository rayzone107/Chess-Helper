package com.rachitgoyal.chesshelper.feature.overlay.service

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

internal object OverlayWindowPositioning {
    fun clampPosition(
        proposedOffset: IntOffset,
        overlaySize: IntSize,
        screenSize: IntSize,
        minimumVisibleStripPx: Int,
    ): IntOffset {
        if (overlaySize == IntSize.Zero || screenSize == IntSize.Zero) return proposedOffset

        val minVisible = minimumVisibleStripPx
            .coerceAtLeast(1)
            .coerceAtMost(overlaySize.width.coerceAtMost(overlaySize.height).coerceAtLeast(1))

        val minX = -(overlaySize.width - minVisible)
        val maxX = screenSize.width - minVisible
        val minY = -(overlaySize.height - minVisible)
        val maxY = screenSize.height - minVisible

        return IntOffset(
            x = proposedOffset.x.coerceIn(minX, maxX),
            y = proposedOffset.y.coerceIn(minY, maxY),
        )
    }
}
