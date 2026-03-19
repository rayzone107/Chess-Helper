package com.rachitgoyal.chesshelper.feature.overlay.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rachitgoyal.chesshelper.domain.chess.model.MoveRecord
import com.rachitgoyal.chesshelper.domain.chess.model.Piece
import com.rachitgoyal.chesshelper.domain.chess.model.Side

@Composable
fun ChessBoard(
    board: Map<String, Piece>,
    selectedSquare: String?,
    legalTargets: Set<String>,
    lastMove: MoveRecord?,
    recommendedMove: MoveRecord?,
    bottomSide: Side,
    onSquareTapped: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ranks = if (bottomSide == Side.WHITE) (7 downTo 0).toList() else (0..7).toList()
    val files = if (bottomSide == Side.WHITE) (0..7).toList() else (7 downTo 0).toList()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
    ) {
        Column(modifier = Modifier.matchParentSize()) {
            for (rank in ranks) {
                Row(modifier = Modifier.weight(1f)) {
                    for (file in files) {
                        val squareId = "${('a'.code + file).toChar()}${rank + 1}"
                        val isSelected = selectedSquare == squareId
                        val isLastMove = squareId == lastMove?.from || squareId == lastMove?.to
                        val baseColor = if ((file + rank) % 2 == 0) Color(0xFFE7D0AE) else Color(0xFF7C5331)
                        val backgroundColor = when {
                            isSelected -> Color(0xFF2563EB)
                            squareId in legalTargets -> Color(0xFFB8E1FF)
                            isLastMove -> Color(0xFFFACC15)
                            else -> baseColor
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(backgroundColor),
                        )
                    }
                }
            }
        }

        if (recommendedMove != null) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val fromCenter = squareCenter(recommendedMove.from, bottomSide, size.width)
                val toCenter = squareCenter(recommendedMove.to, bottomSide, size.width)
                val arrowColor = Color(0xFF22C55E)
                val lineWidth = size.width * 0.02f

                drawLine(
                    color = arrowColor,
                    start = fromCenter,
                    end = toCenter,
                    strokeWidth = lineWidth,
                    cap = StrokeCap.Round,
                )

                val angle = kotlin.math.atan2(toCenter.y - fromCenter.y, toCenter.x - fromCenter.x)
                val arrowHeadLength = size.width * 0.05f
                val left = Offset(
                    x = toCenter.x - arrowHeadLength * kotlin.math.cos(angle - Math.PI / 6).toFloat(),
                    y = toCenter.y - arrowHeadLength * kotlin.math.sin(angle - Math.PI / 6).toFloat(),
                )
                val right = Offset(
                    x = toCenter.x - arrowHeadLength * kotlin.math.cos(angle + Math.PI / 6).toFloat(),
                    y = toCenter.y - arrowHeadLength * kotlin.math.sin(angle + Math.PI / 6).toFloat(),
                )

                drawLine(color = arrowColor, start = toCenter, end = left, strokeWidth = lineWidth, cap = StrokeCap.Round)
                drawLine(color = arrowColor, start = toCenter, end = right, strokeWidth = lineWidth, cap = StrokeCap.Round)
                drawCircle(color = arrowColor.copy(alpha = 0.24f), radius = size.width * 0.05f, center = fromCenter)
                drawCircle(color = arrowColor.copy(alpha = 0.24f), radius = size.width * 0.05f, center = toCenter)
            }
        }

        Column(modifier = Modifier.matchParentSize()) {
            for (rank in ranks) {
                Row(modifier = Modifier.weight(1f)) {
                    for (file in files) {
                        val squareId = "${('a'.code + file).toChar()}${rank + 1}"
                        val piece = board[squareId]
                        val isLegalTarget = squareId in legalTargets
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable { onSquareTapped(squareId) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (piece != null) {
                                Text(
                                    text = piece.symbol,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = if (piece.side == Side.WHITE) Color.White else Color(0xFF0F172A),
                                )
                            }
                            if (isLegalTarget) {
                                Canvas(modifier = Modifier.matchParentSize().padding(10.dp)) {
                                    if (piece == null) {
                                        drawCircle(color = Color(0xCC0F172A), radius = size.minDimension * 0.12f)
                                    } else {
                                        drawCircle(
                                            color = Color(0xFF0F172A),
                                            radius = size.minDimension * 0.32f,
                                            style = Stroke(width = size.minDimension * 0.06f),
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if ((bottomSide == Side.WHITE && rank == 0) || (bottomSide == Side.BLACK && rank == 7)) {
                                    ('a'.code + file).toChar().toString()
                                } else if ((bottomSide == Side.WHITE && file == 0) || (bottomSide == Side.BLACK && file == 7)) {
                                    (rank + 1).toString()
                                } else {
                                    ""
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp),
                                fontSize = 10.sp,
                                color = Color(0xAA0F172A),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun squareCenter(squareId: String, bottomSide: Side, boardSize: Float): Offset {
    val file = squareId[0] - 'a'
    val rank = squareId[1].digitToInt() - 1
    val displayColumn = if (bottomSide == Side.WHITE) file else 7 - file
    val displayRow = if (bottomSide == Side.WHITE) 7 - rank else rank
    val cellSize = boardSize / 8f
    return Offset(
        x = (displayColumn + 0.5f) * cellSize,
        y = (displayRow + 0.5f) * cellSize,
    )
}

