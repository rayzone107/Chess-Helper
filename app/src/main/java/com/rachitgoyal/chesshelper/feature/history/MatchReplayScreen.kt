package com.rachitgoyal.chesshelper.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rachitgoyal.chesshelper.domain.chess.ChessRules
import com.rachitgoyal.chesshelper.domain.chess.FenParser
import com.rachitgoyal.chesshelper.domain.chess.model.BoardTheme
import com.rachitgoyal.chesshelper.domain.chess.model.MatchRecord
import com.rachitgoyal.chesshelper.domain.chess.model.MoveRecord
import com.rachitgoyal.chesshelper.domain.chess.model.ChessPosition
import com.rachitgoyal.chesshelper.feature.overlay.components.ChessBoard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MatchReplayContent(
    match: MatchRecord,
) {
    // Step index: 0 = initial position, 1 = after move 1, etc.
    var step by remember { mutableIntStateOf(0) }
    val totalSteps = match.moves.size

    // Precompute all positions
    val positions: List<ChessPosition> = remember(match.id) {
        val startPos = match.startingFen
            ?.let { FenParser.parse(it).getOrNull() }
            ?: ChessRules.initialPosition()
        val list = mutableListOf(startPos)
        var pos = startPos
        for (move in match.moves) {
            pos = ChessRules.applyMove(pos, move)
            list.add(pos)
        }
        list
    }

    val currentPosition = positions[step]
    val lastMove: MoveRecord? = if (step > 0) match.moves[step - 1] else null
    val checkedKingSquare = ChessRules.checkedKingSquare(currentPosition)

    val dateFormat = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(match.timestampMillis))

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        // Match info subtitle
        Text(
            text = "${match.result.label} • $dateStr • ${match.moveCount} moves",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.size(12.dp))

        // Board
        ChessBoard(
            board = currentPosition.board,
            selectedSquare = null,
            legalTargets = emptySet(),
            lastMove = lastMove,
            checkedKingSquare = checkedKingSquare,
            recommendedMove = null,
            bottomSide = match.assistedSide,
            onSquareTapped = {},
            boardTheme = BoardTheme.CLASSIC,
        )

        Spacer(modifier = Modifier.size(12.dp))

        // Step indicator
        Text(
            text = if (step == 0) "Starting position" else "Move $step of $totalSteps: ${lastMove?.notation ?: ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.size(12.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = { step = 0 },
                enabled = step > 0,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("⏮")
            }
            FilledTonalButton(
                onClick = { step = (step - 1).coerceAtLeast(0) },
                enabled = step > 0,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("◀ Prev")
            }
            FilledTonalButton(
                onClick = { step = (step + 1).coerceAtMost(totalSteps) },
                enabled = step < totalSteps,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Next ▶")
            }
            FilledTonalButton(
                onClick = { step = totalSteps },
                enabled = step < totalSteps,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("⏭")
            }
        }
    }
}
