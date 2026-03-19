package com.rachitgoyal.chesshelper.feature.overlay.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import com.rachitgoyal.chesshelper.feature.overlay.OverlayBoardUiState
import com.rachitgoyal.chesshelper.feature.overlay.PanelMode

@Composable
fun OverlayPanel(
    uiState: OverlayBoardUiState,
    onSquareTapped: (String) -> Unit,
    onRecommendClicked: () -> Unit,
    onApplyRecommendationClicked: () -> Unit,
    onUndoClicked: () -> Unit,
    onResetBoard: () -> Unit,
    onTogglePanelMode: () -> Unit,
    onAssistedSideChanged: (Side) -> Unit,
    onRootBoundsChanged: (IntSize) -> Unit,
    onPanelSizeChanged: (IntSize) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val dragModifier = Modifier.pointerInput(uiState.panelMode) {
        detectDragGestures(
            onDragStart = { onDragStart() },
            onDragEnd = onDragEnd,
            onDragCancel = onDragEnd,
        ) { change, dragAmount ->
            change.consume()
            onDrag(dragAmount)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged(onRootBoundsChanged),
    ) {
        OverlayWindowCard(
            uiState = uiState,
            onSquareTapped = onSquareTapped,
            onRecommendClicked = onRecommendClicked,
            onApplyRecommendationClicked = onApplyRecommendationClicked,
            onUndoClicked = onUndoClicked,
            onResetBoard = onResetBoard,
            onTogglePanelMode = onTogglePanelMode,
            onAssistedSideChanged = onAssistedSideChanged,
            dragHandleModifier = dragModifier,
            modifier = Modifier
                .offset { uiState.panelOffsetPx }
                .onSizeChanged(onPanelSizeChanged),
        )
    }
}

@Composable
fun OverlayWindowCard(
    uiState: OverlayBoardUiState,
    onSquareTapped: (String) -> Unit,
    onRecommendClicked: () -> Unit,
    onApplyRecommendationClicked: () -> Unit,
    onUndoClicked: () -> Unit,
    onResetBoard: () -> Unit,
    onTogglePanelMode: () -> Unit,
    onAssistedSideChanged: (Side) -> Unit,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier,
    onCloseOverlay: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.sizeIn(maxWidth = 420.dp),
        shape = RoundedCornerShape(if (uiState.panelMode == PanelMode.EXPANDED) 24.dp else 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (uiState.isDragging) 14.dp else 8.dp),
    ) {
        if (uiState.panelMode == PanelMode.MINIMIZED) {
            Row(
                modifier = dragHandleModifier
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Color to move: ${uiState.sideToMove.displayName}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                )
                Text(
                    text = uiState.compactRecommendationStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                )
                WindowActionButton(symbol = "▢", contentDescription = "Expand overlay", onClick = onTogglePanelMode)
                if (onCloseOverlay != null) {
                    WindowActionButton(symbol = "✕", contentDescription = "Close overlay", onClick = onCloseOverlay)
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(14.dp),
            ) {
                Row(
                    modifier = dragHandleModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFF1E293B),
                    ) {
                        Text(
                            text = "Color to move: ${uiState.sideToMove.displayName}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                        )
                    }
                    Text(
                        text = uiState.compactRecommendationStatus,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WindowActionButton(symbol = "—", contentDescription = "Minimize overlay", onClick = onTogglePanelMode)
                        if (onCloseOverlay != null) {
                            WindowActionButton(symbol = "✕", contentDescription = "Close overlay", onClick = onCloseOverlay)
                        }
                    }
                }

                ChessBoard(
                    board = uiState.board,
                    selectedSquare = uiState.selectedSquare,
                    legalTargets = uiState.legalTargets,
                    lastMove = uiState.lastMove,
                    recommendedMove = uiState.activeRecommendedMove,
                    bottomSide = uiState.boardBottomSide,
                    onSquareTapped = onSquareTapped,
                )

                RecommendationBanner(uiState = uiState)

                HorizontalDivider(color = Color(0xFF1E293B))

                OverlayControls(
                    uiState = uiState,
                    onRecommendClicked = onRecommendClicked,
                    onApplyRecommendationClicked = onApplyRecommendationClicked,
                    onUndoClicked = onUndoClicked,
                    onResetBoard = onResetBoard,
                    onAssistedSideChanged = onAssistedSideChanged,
                )
            }
        }
    }
}

@Composable
private fun RecommendationBanner(uiState: OverlayBoardUiState) {
    Surface(
        color = Color(0xFF111827),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text = uiState.compactRecommendationStatus,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFE2E8F0),
        )
    }
}

@Composable
private fun WindowActionButton(
    symbol: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E293B),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(40.dp)
                .semantics { this.contentDescription = contentDescription },
        ) {
            Text(
                text = symbol,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}


