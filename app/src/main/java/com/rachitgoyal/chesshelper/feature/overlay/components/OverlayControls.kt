package com.rachitgoyal.chesshelper.feature.overlay.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import com.rachitgoyal.chesshelper.feature.overlay.OverlayBoardUiState

@Composable
fun OverlayControls(
    uiState: OverlayBoardUiState,
    onRecommendClicked: () -> Unit,
    onApplyRecommendationClicked: () -> Unit,
    onUndoClicked: () -> Unit,
    onResetBoard: () -> Unit,
    onAssistedSideChanged: (Side) -> Unit,
    onCopyFenClicked: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "You play",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Side.entries.forEach { side ->
                    val selected = side == uiState.assistedSide
                    val buttonModifier = Modifier.weight(1f)
                    if (selected) {
                        FilledTonalButton(
                            onClick = { onAssistedSideChanged(side) },
                            modifier = buttonModifier,
                        ) {
                            Text(side.displayName)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onAssistedSideChanged(side) },
                            modifier = buttonModifier,
                        ) {
                            Text(side.displayName)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onRecommendClicked,
                enabled = uiState.canRecommend,
                modifier = Modifier.weight(1f),
            ) {
                if (uiState.recommendationState == com.rachitgoyal.chesshelper.feature.overlay.RecommendationState.LOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Analyzing…")
                } else {
                    Text("Show best move")
                }
            }
            OutlinedButton(
                onClick = onUndoClicked,
                enabled = uiState.canUndo,
                modifier = Modifier.weight(1f),
            ) {
                Text("Undo")
            }
        }

        // Only show the manual "Play suggested move" button when auto-apply is OFF.
        if (!uiState.autoApplyBestMove) {
            Button(
                onClick = onApplyRecommendationClicked,
                enabled = uiState.canApplyRecommendation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Play suggested move")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onCopyFenClicked,
                modifier = Modifier.weight(1f),
            ) {
                Text("Copy FEN")
            }
            OutlinedButton(
                onClick = onResetBoard,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp),
            ) {
                Text("New game")
            }
        }
    }
}
