package com.rachitgoyal.chesshelper

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat.startForegroundService
import com.rachitgoyal.chesshelper.feature.overlay.service.OverlayWindowService
import com.rachitgoyal.chesshelper.feature.overlay.service.OverlayWindowServiceState
import com.rachitgoyal.chesshelper.ui.theme.ChessHelperTheme
import com.rachitgoyal.chesshelper.ui.app.ChessHelperApp

class MainActivity : ComponentActivity() {
    private var overlayPermissionGranted by mutableStateOf(false)
    private var pendingOverlayLaunch by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshOverlayPermission()
        setContent {
            ChessHelperTheme {
                ChessHelperApp(
                    overlayPermissionGranted = overlayPermissionGranted,
                    overlayRunning = OverlayWindowServiceState.isRunning,
                    onLaunchOverlay = ::launchOverlay,
                    onStopOverlay = ::stopOverlay,
                    onOpenOverlaySettings = ::openOverlaySettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val hadPendingLaunch = pendingOverlayLaunch
        refreshOverlayPermission()
        if (hadPendingLaunch && overlayPermissionGranted) {
            pendingOverlayLaunch = false
            startOverlayServiceAndBackground()
        }
    }

    private fun refreshOverlayPermission() {
        overlayPermissionGranted = Settings.canDrawOverlays(this)
    }

    private fun launchOverlay() {
        if (!overlayPermissionGranted) {
            pendingOverlayLaunch = true
            openOverlaySettings()
            return
        }
        pendingOverlayLaunch = false
        startOverlayServiceAndBackground()
    }

    private fun stopOverlay() {
        startService(OverlayWindowService.createIntent(this, OverlayWindowService.ACTION_HIDE_OVERLAY))
    }

    private fun openOverlaySettings() {
        startActivity(OverlayWindowService.overlaySettingsIntent(packageName))
    }

    private fun startOverlayServiceAndBackground() {
        startForegroundService(this, OverlayWindowService.createIntent(this, OverlayWindowService.ACTION_SHOW_OVERLAY))
        moveTaskToBack(true)
    }
}
