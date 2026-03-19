package com.rachitgoyal.chesshelper.feature.overlay.service

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayWindowPositioningTest {

    @Test
    fun allowsOverlayToMostlyLeaveScreenOnLeftAndTopWhileKeepingVisibleStrip() {
        val result = OverlayWindowPositioning.clampPosition(
            proposedOffset = IntOffset(x = -500, y = -600),
            overlaySize = IntSize(width = 300, height = 260),
            screenSize = IntSize(width = 1080, height = 2400),
            minimumVisibleStripPx = 48,
        )

        assertEquals(IntOffset(x = -252, y = -212), result)
    }

    @Test
    fun allowsOverlayToMostlyLeaveScreenOnRightAndBottomWhileKeepingVisibleStrip() {
        val result = OverlayWindowPositioning.clampPosition(
            proposedOffset = IntOffset(x = 1400, y = 2600),
            overlaySize = IntSize(width = 320, height = 280),
            screenSize = IntSize(width = 1080, height = 2400),
            minimumVisibleStripPx = 48,
        )

        assertEquals(IntOffset(x = 1032, y = 2352), result)
    }

    @Test
    fun keepsOffsetUnchangedWhenAlreadyWithinPartialVisibilityRange() {
        val result = OverlayWindowPositioning.clampPosition(
            proposedOffset = IntOffset(x = -120, y = 300),
            overlaySize = IntSize(width = 360, height = 300),
            screenSize = IntSize(width = 1080, height = 2400),
            minimumVisibleStripPx = 48,
        )

        assertEquals(IntOffset(x = -120, y = 300), result)
    }
}
