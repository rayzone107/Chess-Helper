package com.rachitgoyal.chesshelper.feature.overlay

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
import com.rachitgoyal.chesshelper.feature.overlay.components.OverlayWindowCard

@Composable
fun OverlayWindowContent(
    viewModel: OverlayBoardViewModel,
    onRequestClose: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val uiState = viewModel.uiState
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

    OverlayWindowCard(
        uiState = uiState,
        onSquareTapped = viewModel::onSquareTapped,
        onRecommendClicked = viewModel::onRecommendClicked,
        onApplyRecommendationClicked = viewModel::onApplyRecommendationClicked,
        onUndoClicked = viewModel::onUndoClicked,
        onResetBoard = viewModel::onResetBoard,
        onTogglePanelMode = viewModel::togglePanelMode,
        onToggleMoveHistoryExpanded = viewModel::toggleMoveHistoryExpanded,
        onAssistedSideChanged = viewModel::onAssistedSideChanged,
        onCopyFenClicked = viewModel::onCopyFenClicked,
        onFenCopyConsumed = viewModel::onFenCopyConsumed,
        onHapticConsumed = viewModel::onHapticConsumed,
        onSoundEventConsumed = viewModel::onSoundEventConsumed,
        onLoadFen = viewModel::onLoadFen,
        onFenLoadErrorConsumed = viewModel::onFenLoadErrorConsumed,
        dragHandleModifier = dragModifier,
        onCloseOverlay = { viewModel.saveAndClose(onRequestClose) },
    )
}
