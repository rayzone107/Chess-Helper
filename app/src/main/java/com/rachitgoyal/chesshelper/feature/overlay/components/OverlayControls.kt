package com.rachitgoyal.chesshelper.feature.overlay.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import com.rachitgoyal.chesshelper.feature.overlay.OverlayBoardUiState
import com.rachitgoyal.chesshelper.feature.overlay.RecommendationState

private val ControlSurfaceColor = Color(0xEB1E293B)
private val ControlSelectedColor = Color(0xFF2563EB)

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
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Play as",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SideSegmentedToggle(
                selectedSide = uiState.assistedSide,
                onSideSelected = onAssistedSideChanged,
                modifier = Modifier.weight(1f),
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ControlSurfaceColor,
            ) {
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = { menuExpanded = true }
                ) {
                    Text("⋯", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
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
                        text = { Text("New game") },
                        onClick = {
                            menuExpanded = false
                            onResetBoard()
                        },
                    )
                }
            }
        }

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
                disabledContainerColor = ControlSurfaceColor.copy(alpha = 0.84f),
                disabledContentColor = Color(0xFF94A3B8),
            ),
            modifier = Modifier.fillMaxWidth(),
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
    }
}

@Composable
private fun SideSegmentedToggle(
    selectedSide: Side,
    onSideSelected: (Side) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = ControlSurfaceColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Side.entries.forEach { side ->
                val selected = selectedSide == side
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) ControlSelectedColor else Color.Transparent,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSideSelected(side) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = side.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color.White else Color(0xFFE2E8F0),
                        )
                    }
                }
            }
        }
    }
}
