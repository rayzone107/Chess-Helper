package com.rachitgoyal.chesshelper.feature.overlay.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rachitgoyal.chesshelper.domain.chess.model.MoveRecord
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import com.rachitgoyal.chesshelper.domain.chess.model.GameStatus
import com.rachitgoyal.chesshelper.feature.overlay.BoardHapticEvent
import com.rachitgoyal.chesshelper.feature.overlay.OverlayBoardUiState
import com.rachitgoyal.chesshelper.feature.overlay.PanelMode

private val OverlayCardColor = Color(0xCC0F172A)
private val OverlaySurfaceColor = Color(0xB31E293B)
private val OverlayDividerColor = Color(0x801E293B)
private val OverlaySecondaryText = Color(0xFFCBD5E1)

@Composable
fun OverlayPanel(
    uiState: OverlayBoardUiState,
    onSquareTapped: (String) -> Unit,
    onRecommendClicked: () -> Unit,
    onApplyRecommendationClicked: () -> Unit,
    onUndoClicked: () -> Unit,
    onResetBoard: () -> Unit,
    onTogglePanelMode: () -> Unit,
    onToggleMoveHistoryExpanded: () -> Unit,
    onAssistedSideChanged: (Side) -> Unit,
    onRootBoundsChanged: (IntSize) -> Unit,
    onPanelSizeChanged: (IntSize) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onCopyFenClicked: () -> Unit,
    onFenCopyConsumed: () -> Unit,
    onHapticConsumed: () -> Unit,
    onSoundEventConsumed: () -> Unit,
    onLoadFen: (String) -> Unit,
    onFenLoadErrorConsumed: () -> Unit,
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
            onToggleMoveHistoryExpanded = onToggleMoveHistoryExpanded,
            onAssistedSideChanged = onAssistedSideChanged,
            dragHandleModifier = dragModifier,
            onCopyFenClicked = onCopyFenClicked,
            onFenCopyConsumed = onFenCopyConsumed,
            onHapticConsumed = onHapticConsumed,
            onSoundEventConsumed = onSoundEventConsumed,
            onLoadFen = onLoadFen,
            onFenLoadErrorConsumed = onFenLoadErrorConsumed,
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
    onToggleMoveHistoryExpanded: () -> Unit,
    onAssistedSideChanged: (Side) -> Unit,
    dragHandleModifier: Modifier,
    onCopyFenClicked: () -> Unit,
    onFenCopyConsumed: () -> Unit,
    onHapticConsumed: () -> Unit,
    onSoundEventConsumed: () -> Unit,
    onLoadFen: (String) -> Unit,
    onFenLoadErrorConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    onCloseOverlay: (() -> Unit)? = null,
) {
    // --- Side effects ---
    val context = LocalContext.current

    LaunchedEffect(uiState.fenCopied) {
        if (uiState.fenCopied) {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard?.setPrimaryClip(ClipData.newPlainText("FEN", uiState.currentFen))
            Toast.makeText(context.applicationContext, "FEN copied to clipboard", Toast.LENGTH_SHORT).show()
            onFenCopyConsumed()
        }
    }

    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(uiState.hapticEvent) {
        val event = uiState.hapticEvent ?: return@LaunchedEffect
        when (event) {
            BoardHapticEvent.MOVE -> hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            BoardHapticEvent.CAPTURE, BoardHapticEvent.CHECK -> hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        onHapticConsumed()
    }

    val audioManager = context.getSystemService(android.media.AudioManager::class.java)
    LaunchedEffect(uiState.soundEvent) {
        val event = uiState.soundEvent ?: return@LaunchedEffect
        if (uiState.enableSoundEffects) {
            when (event) {
                BoardHapticEvent.MOVE -> audioManager?.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK, 1f)
                BoardHapticEvent.CAPTURE -> audioManager?.playSoundEffect(android.media.AudioManager.FX_KEYPRESS_DELETE, 1f)
                BoardHapticEvent.CHECK -> audioManager?.playSoundEffect(android.media.AudioManager.FX_KEYPRESS_RETURN, 1f)
            }
        }
        onSoundEventConsumed()
    }

    LaunchedEffect(uiState.fenLoadError) {
        uiState.fenLoadError?.let {
            Toast.makeText(context.applicationContext, "Invalid FEN: $it", Toast.LENGTH_SHORT).show()
            onFenLoadErrorConsumed()
        }
    }

    // Build the Paste FEN lambda — reads clipboard internally so OverlayControls stays side-effect-free
    val clipboardManager = context.getSystemService(ClipboardManager::class.java)
    val onPasteFen: () -> Unit = {
        val clip = clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString()
        if (clip != null) {
            onLoadFen(clip)
        } else {
            Toast.makeText(context.applicationContext, "Nothing to paste — copy a FEN string first", Toast.LENGTH_SHORT).show()
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Close-confirmation state
    var showCloseConfirmation by remember { mutableStateOf(false) }
    val interceptedClose: (() -> Unit)? = onCloseOverlay?.let {
        { showCloseConfirmation = true }
    }

    if (showCloseConfirmation && onCloseOverlay != null) {
        AlertDialog(
            onDismissRequest = { showCloseConfirmation = false },
            title = { Text("Close overlay?") },
            text = { Text("The current game will be saved to match history.") },
            confirmButton = {
                TextButton(onClick = {
                    showCloseConfirmation = false
                    onCloseOverlay()
                }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = { showCloseConfirmation = false }) { Text("Cancel") }
            },
        )
    }

    Card(
        modifier = modifier.sizeIn(maxWidth = 420.dp),
        shape = RoundedCornerShape(if (uiState.panelMode == PanelMode.EXPANDED) 24.dp else 20.dp),
        colors = CardDefaults.cardColors(containerColor = OverlayCardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (uiState.isDragging) 14.dp else 8.dp),
    ) {
        if (uiState.panelMode == PanelMode.MINIMIZED) {
            MinimizedOverlayHeader(
                uiState = uiState,
                dragHandleModifier = dragHandleModifier,
                onTogglePanelMode = onTogglePanelMode,
                onCloseOverlay = interceptedClose,
            )
        } else if (isLandscape) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(0.54f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ExpandedOverlayHeader(
                        uiState = uiState,
                        dragHandleModifier = dragHandleModifier,
                        onTogglePanelMode = onTogglePanelMode,
                        onCloseOverlay = interceptedClose,
                    )
                    ChessBoard(
                        board = uiState.board,
                        selectedSquare = uiState.selectedSquare,
                        legalTargets = uiState.legalTargets,
                        lastMove = uiState.lastMove,
                        checkedKingSquare = uiState.checkedKingSquare,
                        recommendedMove = uiState.activeRecommendedMove,
                        bottomSide = uiState.boardBottomSide,
                        onSquareTapped = onSquareTapped,
                        boardTheme = uiState.boardTheme,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(0.46f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OverlayDetailStack(
                        uiState = uiState,
                        onToggleMoveHistoryExpanded = onToggleMoveHistoryExpanded,
                        onRecommendClicked = onRecommendClicked,
                        onApplyRecommendationClicked = onApplyRecommendationClicked,
                        onUndoClicked = onUndoClicked,
                        onResetBoard = onResetBoard,
                        onAssistedSideChanged = onAssistedSideChanged,
                        onCopyFenClicked = onCopyFenClicked,
                        onPasteFenClicked = onPasteFen,
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(14.dp),
            ) {
                ExpandedOverlayHeader(
                    uiState = uiState,
                    dragHandleModifier = dragHandleModifier,
                    onTogglePanelMode = onTogglePanelMode,
                    onCloseOverlay = interceptedClose,
                )
                ChessBoard(
                    board = uiState.board,
                    selectedSquare = uiState.selectedSquare,
                    legalTargets = uiState.legalTargets,
                    lastMove = uiState.lastMove,
                    checkedKingSquare = uiState.checkedKingSquare,
                    recommendedMove = uiState.activeRecommendedMove,
                    bottomSide = uiState.boardBottomSide,
                    onSquareTapped = onSquareTapped,
                    boardTheme = uiState.boardTheme,
                )
                OverlayDetailStack(
                    uiState = uiState,
                    onToggleMoveHistoryExpanded = onToggleMoveHistoryExpanded,
                    onRecommendClicked = onRecommendClicked,
                    onApplyRecommendationClicked = onApplyRecommendationClicked,
                    onUndoClicked = onUndoClicked,
                    onResetBoard = onResetBoard,
                    onAssistedSideChanged = onAssistedSideChanged,
                    onCopyFenClicked = onCopyFenClicked,
                    onPasteFenClicked = onPasteFen,
                )
            }
        }
    }
}

@Composable
private fun MinimizedOverlayHeader(
    uiState: OverlayBoardUiState,
    dragHandleModifier: Modifier,
    onTogglePanelMode: () -> Unit,
    onCloseOverlay: (() -> Unit)?,
) {
    Row(
        modifier = dragHandleModifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = if (uiState.isCheckmate) "Checkmate" else "${uiState.sideToMove.displayName} to move",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
            color = Color.White,
        )
        Text(
            text = uiState.compactStatusText,
            style = MaterialTheme.typography.bodySmall,
            color = OverlaySecondaryText,
        )
        WindowActionButton(symbol = "▢", contentDescription = "Expand overlay", onClick = onTogglePanelMode)
        if (onCloseOverlay != null) {
            WindowActionButton(symbol = "✕", contentDescription = "Close overlay", onClick = onCloseOverlay)
        }
    }
}

@Composable
private fun ExpandedOverlayHeader(
    uiState: OverlayBoardUiState,
    dragHandleModifier: Modifier,
    onTogglePanelMode: () -> Unit,
    onCloseOverlay: (() -> Unit)?,
) {
    Row(
        modifier = dragHandleModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = OverlaySurfaceColor,
        ) {
            Text(
                text = if (uiState.isCheckmate) "Checkmate" else "${uiState.sideToMove.displayName} to move",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
        Text(
            text = uiState.compactStatusText,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = OverlaySecondaryText,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WindowActionButton(symbol = "—", contentDescription = "Minimize overlay", onClick = onTogglePanelMode)
            if (onCloseOverlay != null) {
                WindowActionButton(symbol = "✕", contentDescription = "Close overlay", onClick = onCloseOverlay)
            }
        }
    }
}

@Composable
private fun OverlayDetailStack(
    uiState: OverlayBoardUiState,
    onToggleMoveHistoryExpanded: () -> Unit,
    onRecommendClicked: () -> Unit,
    onApplyRecommendationClicked: () -> Unit,
    onUndoClicked: () -> Unit,
    onResetBoard: () -> Unit,
    onAssistedSideChanged: (Side) -> Unit,
    onCopyFenClicked: () -> Unit,
    onPasteFenClicked: () -> Unit,
) {
    val shouldShowBanner =
        uiState.gameStatus != GameStatus.NORMAL ||
            (uiState.recommendationState != com.rachitgoyal.chesshelper.feature.overlay.RecommendationState.IDLE &&
                uiState.recommendationState != com.rachitgoyal.chesshelper.feature.overlay.RecommendationState.LOADING) ||
            uiState.activeRecommendedMove != null ||
            uiState.recommendationError != null

    if (shouldShowBanner) {
        RecommendationBanner(uiState = uiState)
    }
    if (uiState.isMoveHistoryExpanded && uiState.moveHistory.isNotEmpty()) {
        MoveHistoryPanel(movePairs = uiState.moveHistory.chunked(2))
    }
    HorizontalDivider(color = OverlayDividerColor)
    OverlayControls(
        uiState = uiState,
        onRecommendClicked = onRecommendClicked,
        onApplyRecommendationClicked = onApplyRecommendationClicked,
        onUndoClicked = onUndoClicked,
        onResetBoard = onResetBoard,
        onToggleMoveHistoryExpanded = onToggleMoveHistoryExpanded,
        onAssistedSideChanged = onAssistedSideChanged,
        onCopyFenClicked = onCopyFenClicked,
        onPasteFenClicked = onPasteFenClicked,
    )
}


@Composable
private fun MoveHistoryPanel(movePairs: List<List<MoveRecord>>) {
    val listState = rememberLazyListState()

    LaunchedEffect(movePairs.size) {
        if (movePairs.isNotEmpty()) {
            listState.animateScrollToItem(movePairs.size - 1)
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = OverlaySurfaceColor,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 88.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(movePairs) { index, pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OverlaySecondaryText,
                        modifier = Modifier.width(28.dp),
                    )
                    Text(
                        text = pair[0].notation,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = pair.getOrNull(1)?.notation ?: "…",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (pair.size > 1) Color.White else OverlaySecondaryText,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationBanner(uiState: OverlayBoardUiState) {
    val containerColor = when {
        uiState.gameStatus == GameStatus.CHECKMATE -> Color(0xB3DC2626)
        uiState.gameStatus == GameStatus.STALEMATE -> Color(0xB3475563)
        uiState.isRecommendationBannerError -> Color(0xB37F1D1D)
        uiState.gameStatus == GameStatus.CHECK -> Color(0xB37C2D12)
        else -> OverlaySurfaceColor
    }
    val textColor = when {
        uiState.gameStatus == GameStatus.CHECKMATE -> Color.White
        uiState.isRecommendationBannerError -> Color(0xFFFCA5A5)
        else -> Color(0xFFE2E8F0)
    }
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text = uiState.recommendationBannerText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = if (uiState.isCheckmate) 12.dp else 10.dp),
            style = if (uiState.isCheckmate) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            color = textColor,
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
        color = OverlaySurfaceColor,
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
