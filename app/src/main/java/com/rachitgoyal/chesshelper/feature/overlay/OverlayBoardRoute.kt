package com.rachitgoyal.chesshelper.feature.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.rachitgoyal.chesshelper.feature.overlay.components.OverlayPanel
import com.rachitgoyal.chesshelper.feature.overlay.components.OverlayWindowCard

@Composable
fun OverlayBoardRoute(
    viewModel: OverlayBoardViewModel? = null,
) {
    val context = LocalContext.current.applicationContext
    val factory = remember(context) { OverlayBoardViewModelFactory(context) }
    val resolvedViewModel = viewModel ?: composeViewModel(factory = factory)
    val uiState = resolvedViewModel.uiState

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = "Chess Overlay Assistant",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )

        OverlayPanel(
            uiState = uiState,
            onSquareTapped = resolvedViewModel::onSquareTapped,
            onRecommendClicked = resolvedViewModel::onRecommendClicked,
            onApplyRecommendationClicked = resolvedViewModel::onApplyRecommendationClicked,
            onUndoClicked = resolvedViewModel::onUndoClicked,
            onResetBoard = resolvedViewModel::onResetBoard,
            onTogglePanelMode = resolvedViewModel::togglePanelMode,
            onAssistedSideChanged = resolvedViewModel::onAssistedSideChanged,
            onRootBoundsChanged = resolvedViewModel::onRootBoundsChanged,
            onPanelSizeChanged = resolvedViewModel::onPanelSizeChanged,
            onDragStart = resolvedViewModel::onDragStart,
            onDrag = resolvedViewModel::onDrag,
            onDragEnd = resolvedViewModel::onDragEnd,
        )
    }
}

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
        onAssistedSideChanged = viewModel::onAssistedSideChanged,
        dragHandleModifier = dragModifier,
        onCloseOverlay = onRequestClose,
    )
}


