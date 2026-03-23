package com.rachitgoyal.chesshelper.feature.overlay.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.unit.sp
import com.rachitgoyal.chesshelper.domain.chess.model.MoveRecord
import com.rachitgoyal.chesshelper.domain.chess.model.Piece
import com.rachitgoyal.chesshelper.domain.chess.model.PieceType
import com.rachitgoyal.chesshelper.domain.chess.model.GameStatus
import com.rachitgoyal.chesshelper.domain.chess.model.Side
import com.rachitgoyal.chesshelper.feature.overlay.BoardHapticEvent
import com.rachitgoyal.chesshelper.feature.overlay.OverlayBoardUiState
import com.rachitgoyal.chesshelper.feature.overlay.PanelMode
import com.rachitgoyal.chesshelper.ui.theme.OverlayColors

// Aliases for brevity — all values come from the shared design-system object.
private val OverlayCardColor    = OverlayColors.CardBackground
private val OverlaySurfaceColor = OverlayColors.Surface
private val OverlayDividerColor = OverlayColors.Divider
private val OverlaySecondaryText = OverlayColors.SecondaryText
private val ConfigModeAccent    = OverlayColors.ConfigAccent
private val ConfigModeCardColor = OverlayColors.ConfigCardBackground

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
    onEnterConfigMode: () -> Unit = {},
    onExitConfigMode: () -> Unit = {},
    onDiscardConfigChanges: () -> Unit = {},
    onConfigUndo: () -> Unit = {},
    onConfigRedo: () -> Unit = {},
    onConfigRemoveSelected: () -> Unit = {},
    onConfigClearBoard: () -> Unit = {},
    onConfigResetToStart: () -> Unit = {},
    onConfigToggleSideToMove: () -> Unit = {},
    onConfigSelectCatalogPiece: (Piece?) -> Unit = {},
    onConfigDismissValidationError: () -> Unit = {},
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

    Card(
        modifier = modifier
            .sizeIn(maxWidth = 420.dp)
            .alpha(uiState.overlayOpacity),
        shape = RoundedCornerShape(if (uiState.panelMode == PanelMode.EXPANDED) 24.dp else 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.isConfigMode) ConfigModeCardColor else OverlayCardColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (uiState.isDragging) 14.dp else 8.dp),
    ) {
        if (showCloseConfirmation) {
            // Inline confirmation rendered inside the overlay card (no Dialog window)
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Close overlay?",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    text = "The current game will be saved to match history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OverlaySecondaryText,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { showCloseConfirmation = false }) {
                        Text("Cancel", color = Color.White)
                    }
                    OutlinedButton(
                        onClick = {
                            showCloseConfirmation = false
                            onCloseOverlay?.invoke()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OverlayColors.CheckBorder),
                    ) {
                        Text("Close")
                    }
                }
            }
        } else {
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
                        selectedSquare = if (uiState.isConfigMode) uiState.configSelectedSquare else uiState.selectedSquare,
                        legalTargets = if (uiState.isConfigMode) emptySet() else uiState.legalTargets,
                        lastMove = if (uiState.isConfigMode) null else uiState.lastMove,
                        checkedKingSquare = if (uiState.isConfigMode) null else uiState.checkedKingSquare,
                        recommendedMove = if (uiState.isConfigMode) null else uiState.activeRecommendedMove,
                        bottomSide = uiState.boardBottomSide,
                        onSquareTapped = onSquareTapped,
                        boardTheme = uiState.boardTheme,
                        modifier = Modifier.alpha(uiState.boardOpacity),
                        isConfigMode = uiState.isConfigMode,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(0.46f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (uiState.isConfigMode) {
                        ConfigModeControls(
                            uiState = uiState,
                            onExitConfigMode = onExitConfigMode,
                            onDiscardConfigChanges = onDiscardConfigChanges,
                            onConfigUndo = onConfigUndo,
                            onConfigRedo = onConfigRedo,
                            onConfigRemoveSelected = onConfigRemoveSelected,
                            onConfigClearBoard = onConfigClearBoard,
                            onConfigResetToStart = onConfigResetToStart,
                            onConfigToggleSideToMove = onConfigToggleSideToMove,
                            onConfigSelectCatalogPiece = onConfigSelectCatalogPiece,
                            onConfigDismissValidationError = onConfigDismissValidationError,
                        )
                    } else {
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
                            onEnterConfigMode = onEnterConfigMode,
                        )
                    }
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
                    selectedSquare = if (uiState.isConfigMode) uiState.configSelectedSquare else uiState.selectedSquare,
                    legalTargets = if (uiState.isConfigMode) emptySet() else uiState.legalTargets,
                    lastMove = if (uiState.isConfigMode) null else uiState.lastMove,
                    checkedKingSquare = if (uiState.isConfigMode) null else uiState.checkedKingSquare,
                    recommendedMove = if (uiState.isConfigMode) null else uiState.activeRecommendedMove,
                    bottomSide = uiState.boardBottomSide,
                    onSquareTapped = onSquareTapped,
                    boardTheme = uiState.boardTheme,
                    modifier = Modifier.alpha(uiState.boardOpacity),
                    isConfigMode = uiState.isConfigMode,
                )
                if (uiState.isConfigMode) {
                    ConfigModeControls(
                        uiState = uiState,
                        onExitConfigMode = onExitConfigMode,
                        onDiscardConfigChanges = onDiscardConfigChanges,
                        onConfigUndo = onConfigUndo,
                        onConfigRedo = onConfigRedo,
                        onConfigRemoveSelected = onConfigRemoveSelected,
                        onConfigClearBoard = onConfigClearBoard,
                        onConfigResetToStart = onConfigResetToStart,
                        onConfigToggleSideToMove = onConfigToggleSideToMove,
                        onConfigSelectCatalogPiece = onConfigSelectCatalogPiece,
                        onConfigDismissValidationError = onConfigDismissValidationError,
                    )
                } else {
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
                        onEnterConfigMode = onEnterConfigMode,
                    )
                }
            }
        }
        } // end else (not showing close confirmation)
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
            color = if (uiState.isConfigMode) ConfigModeAccent.copy(alpha = 0.3f) else OverlaySurfaceColor,
        ) {
            Text(
                text = when {
                    uiState.isConfigMode -> "⚙ Board Setup"
                    uiState.isCheckmate -> "Checkmate"
                    else -> "${uiState.sideToMove.displayName} to move"
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = if (uiState.isConfigMode) ConfigModeAccent else Color.White,
            )
        }
        Text(
            text = if (uiState.isConfigMode) "Tap piece, then tap destination" else uiState.compactStatusText,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = if (uiState.isConfigMode) ConfigModeAccent.copy(alpha = 0.7f) else OverlaySecondaryText,
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
    onEnterConfigMode: () -> Unit,
) {
    val shouldShowBanner =
        uiState.gameStatus != GameStatus.NORMAL ||
            (uiState.recommendationState != com.rachitgoyal.chesshelper.feature.overlay.RecommendationState.IDLE &&
                uiState.recommendationState != com.rachitgoyal.chesshelper.feature.overlay.RecommendationState.LOADING) ||
            uiState.activeRecommendedMove != null ||
            uiState.recommendationError != null

    if (shouldShowBanner) {
        RecommendationBanner(
            uiState = uiState,
            onResetBoard = onResetBoard,
        )
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
        onEnterConfigMode = onEnterConfigMode,
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
private fun RecommendationBanner(
    uiState: OverlayBoardUiState,
    onResetBoard: () -> Unit,
) {
    val containerColor = when {
        uiState.gameStatus == GameStatus.CHECKMATE -> OverlayColors.Error
        uiState.gameStatus == GameStatus.STALEMATE -> OverlayColors.StalemateBackground
        uiState.isRecommendationBannerError -> OverlayColors.ErrorDark
        uiState.gameStatus == GameStatus.CHECK -> OverlayColors.WarningDark
        else -> OverlaySurfaceColor
    }
    val textColor = when {
        uiState.gameStatus == GameStatus.CHECKMATE -> Color.White
        uiState.isRecommendationBannerError -> OverlayColors.ErrorTextLight
        else -> OverlayColors.InfoText
    }
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
    ) {
        if (uiState.isCheckmate) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Checkmate",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor,
                )
                TextButton(onClick = onResetBoard) {
                    Text("New Game", color = Color.White)
                }
            }
        } else {
            Text(
                text = uiState.recommendationBannerText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
            )
        }
    }
}

@Composable
private fun ConfigModeControls(
    uiState: OverlayBoardUiState,
    onExitConfigMode: () -> Unit,
    onDiscardConfigChanges: () -> Unit,
    onConfigUndo: () -> Unit,
    onConfigRedo: () -> Unit,
    onConfigRemoveSelected: () -> Unit,
    onConfigClearBoard: () -> Unit,
    onConfigResetToStart: () -> Unit,
    onConfigToggleSideToMove: () -> Unit,
    onConfigSelectCatalogPiece: (Piece?) -> Unit,
    onConfigDismissValidationError: () -> Unit,
) {
    var showDiscardConfirmation by remember(uiState.hasConfigChanges) { mutableStateOf(false) }

    // Validation error
    if (uiState.configValidationError != null) {
        Surface(
            color = OverlayColors.Error,
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = uiState.configValidationError,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    onClick = onConfigDismissValidationError,
                ) {
                    Text(
                        "✕",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }

    if (uiState.hasConfigChanges && showDiscardConfirmation) {
        Surface(
            color = OverlayColors.ErrorDark,
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Discard all board setup changes and restore the original position?",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { showDiscardConfirmation = false }) {
                        Text("Keep editing", color = Color.White)
                    }
                    TextButton(onClick = {
                        showDiscardConfirmation = false
                        onDiscardConfigChanges()
                    }) {
                        Text("Discard", color = OverlayColors.DiscardText)
                    }
                }
            }
        }
    }

    Surface(
        color = ConfigModeAccent.copy(alpha = 0.18f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Side-to-move toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Next:",
                    style = MaterialTheme.typography.bodySmall,
                    color = OverlaySecondaryText,
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OverlaySurfaceColor,
                    onClick = onConfigToggleSideToMove,
                    modifier = Modifier.padding(start = 6.dp),
                ) {
                    Text(
                        text = uiState.configSideToMove.displayName,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                }
            }

            // Action buttons — evenly distributed, icon + label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ConfigActionButton("↩", "Undo", uiState.configUndoStack.isNotEmpty(), onConfigUndo, Modifier.weight(1f))
                ConfigActionButton("↪", "Redo", uiState.configRedoStack.isNotEmpty(), onConfigRedo, Modifier.weight(1f))
                ConfigActionButton("✕", "Remove", uiState.configSelectedSquare != null, onConfigRemoveSelected, Modifier.weight(1f))
                ConfigActionButton("⊘", "Clear", true, onConfigClearBoard, Modifier.weight(1f))
                ConfigActionButton("⟲", "Reset", true, onConfigResetToStart, Modifier.weight(1f))
            }

            // Piece catalog — White (inline label)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "W",
                    style = MaterialTheme.typography.labelSmall,
                    color = OverlaySecondaryText,
                    modifier = Modifier.width(16.dp),
                )
                val whitePieces = listOf(PieceType.KING, PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT, PieceType.PAWN)
                whitePieces.forEach { type ->
                    val piece = Piece(Side.WHITE, type)
                    val isSelected = uiState.configCatalogPiece == piece
                    CatalogPieceButton(piece, isSelected, { onConfigSelectCatalogPiece(piece) }, Modifier.weight(1f))
                }
            }

            // Piece catalog — Black (inline label)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "B",
                    style = MaterialTheme.typography.labelSmall,
                    color = OverlaySecondaryText,
                    modifier = Modifier.width(16.dp),
                )
                val blackPieces = listOf(PieceType.KING, PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT, PieceType.PAWN)
                blackPieces.forEach { type ->
                    val piece = Piece(Side.BLACK, type)
                    val isSelected = uiState.configCatalogPiece == piece
                    CatalogPieceButton(piece, isSelected, { onConfigSelectCatalogPiece(piece) }, Modifier.weight(1f))
                }
            }

            // Hint text
            Text(
                text = when {
                    uiState.configCatalogPiece != null -> "Tap a square to place ${uiState.configCatalogPiece.symbol}. Tap piece again to deselect."
                    uiState.configSelectedSquare != null -> "Tap destination to move, or tap again to remove."
                    else -> "Tap a piece on the board, or pick from catalog above."
                },
                style = MaterialTheme.typography.labelSmall,
                color = OverlaySecondaryText.copy(alpha = 0.7f),
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (uiState.hasConfigChanges) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = OverlaySurfaceColor,
                onClick = { showDiscardConfirmation = true },
                modifier = Modifier.weight(0.42f),
            ) {
                Text(
                    text = "Cancel",
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ConfigModeAccent,
            onClick = onExitConfigMode,
            modifier = Modifier.weight(if (uiState.hasConfigChanges) 0.58f else 1f),
        ) {
            Text(
                text = "✓  Done — Set board",
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.titleSmall,
                color = OverlayColors.ConfigDoneText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ConfigActionButton(
    icon: String,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (enabled) OverlaySurfaceColor else OverlaySurfaceColor.copy(alpha = 0.4f),
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                color = if (enabled) OverlaySecondaryText else OverlaySecondaryText.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun CatalogPieceButton(
    piece: Piece,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) ConfigModeAccent.copy(alpha = 0.5f) else OverlaySurfaceColor,
        onClick = onClick,
        modifier = modifier.then(
            if (isSelected) Modifier.border(1.dp, ConfigModeAccent, RoundedCornerShape(8.dp))
            else Modifier
        ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = piece.symbol,
                fontSize = 22.sp,
                style = MaterialTheme.typography.titleLarge,
                color = if (piece.side == Side.WHITE) OverlayColors.WhitePiece else OverlayColors.BlackPiece,
            )
        }
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
