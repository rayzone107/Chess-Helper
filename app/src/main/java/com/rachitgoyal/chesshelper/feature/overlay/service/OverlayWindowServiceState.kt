package com.rachitgoyal.chesshelper.feature.overlay.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object OverlayWindowServiceState {
    var isRunning by mutableStateOf(false)
        internal set
}

