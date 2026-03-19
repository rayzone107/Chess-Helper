package com.rachitgoyal.chesshelper

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.rachitgoyal.chesshelper.feature.overlay.OverlayBoardRoute
import com.rachitgoyal.chesshelper.ui.theme.ChessHelperTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OverlayPanelUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun overlayPanelCanMinimizeAndExpand() {
        composeRule.setContent {
            ChessHelperTheme {
                OverlayBoardRoute()
            }
        }

        assertTrue(composeRule.onAllNodesWithText("Color to move: White").fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithContentDescription("Minimize overlay").performClick()
        composeRule.onNodeWithContentDescription("Expand overlay").performClick()
        assertTrue(composeRule.onAllNodesWithText("Color to move: White").fetchSemanticsNodes().isNotEmpty())
    }
}



