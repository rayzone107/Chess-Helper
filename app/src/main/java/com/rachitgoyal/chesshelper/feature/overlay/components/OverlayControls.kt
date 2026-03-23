package com.rachitgoyal.chesshelper.feature.overlay.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import com.rachitgoyal.chesshelper.feature.overlay.OverlayBoardUiState
import com.rachitgoyal.chesshelper.feature.overlay.RecommendationState
import com.rachitgoyal.chesshelper.ui.theme.OverlayColors

private val ControlSurfaceColor = OverlayColors.Surface

@Composable
fun OverlayControls(
    uiState: OverlayBoardUiState,
    onRecommendClicked: () -> Unit,
    onApplyRecommendationClicked: () -> Unit,
    onUndoClicked: () -> Unit,
    onResetBoard: () -> Unit,
    onToggleMoveHistoryExpanded: () -> Unit,
    onAssistedSideChanged: (Side) -> Unit,
    onCopyFenClicked: () -> Unit,
    onPasteFenClicked: () -> Unit,
    onEnterConfigMode: () -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalButton(
            onClick = if (!uiState.autoApplyBestMove && uiState.canApplyRecommendation) {
                onApplyRecommendationClicked
            } else {
                onRecommendClicked
            },
            enabled = if (!uiState.autoApplyBestMove && uiState.canApplyRecommendation) {
                uiState.canApplyRecommendation
            } else {
                uiState.canRecommend
            },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = ControlSurfaceColor,
                contentColor = Color.White,
                disabledContainerColor = ControlSurfaceColor,
                disabledContentColor = OverlayColors.DisabledText,
            ),
            modifier = Modifier.weight(1f),
        ) {
            when {
                uiState.recommendationState == RecommendationState.LOADING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Analyzing…")
                }

                !uiState.autoApplyBestMove && uiState.canApplyRecommendation -> Text("Play suggested move")
                else -> Text("Show best move")
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ControlSurfaceColor,
        ) {
            IconButton(
                modifier = Modifier.size(40.dp),
                onClick = { menuExpanded = true },
            ) {
                Text("⋯", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = {
                        Text("Play as ${uiState.assistedSide.opposite().displayName}")
                    },
                    onClick = {
                        menuExpanded = false
                        onAssistedSideChanged(uiState.assistedSide.opposite())
                    },
                )
                DropdownMenuItem(
                    text = { Text(if (uiState.isMoveHistoryExpanded) "Hide moves" else "Show moves") },
                    onClick = {
                        menuExpanded = false
                        onToggleMoveHistoryExpanded()
                    },
                    enabled = uiState.moveHistory.isNotEmpty(),
                )
                DropdownMenuItem(
                    text = { Text("Undo") },
                    onClick = {
                        menuExpanded = false
                        onUndoClicked()
                    },
                    enabled = uiState.canUndo,
                )
                DropdownMenuItem(
                    text = { Text("Copy FEN") },
                    onClick = {
                        menuExpanded = false
                        onCopyFenClicked()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Paste FEN") },
                    onClick = {
                        menuExpanded = false
                        onPasteFenClicked()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Board setup") },
                    onClick = {
                        menuExpanded = false
                        onEnterConfigMode()
                    },
                )
                DropdownMenuItem(
                    text = { Text("New game") },
                    onClick = {
                        menuExpanded = false
                        onResetBoard()
                    },
                )
            }
        }
    }
}
