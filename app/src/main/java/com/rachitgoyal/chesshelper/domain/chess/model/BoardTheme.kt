package com.rachitgoyal.chesshelper.domain.chess.model

import androidx.compose.ui.graphics.Color

enum class BoardTheme(
    val displayName: String,
    val lightSquareColor: Color,
    val darkSquareColor: Color,
) {
    CLASSIC("Classic", Color(0xFFE7D0AE), Color(0xFF7C5331)),
    BLUE("Blue",     Color(0xFFCDD6E0), Color(0xFF4A6FA5)),
    GREEN("Green",   Color(0xFFEEEED2), Color(0xFF769656)),
}

