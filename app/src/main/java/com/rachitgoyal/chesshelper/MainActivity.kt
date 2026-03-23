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
import com.rachitgoyal.chesshelper.data.MatchHistoryRepository
import com.rachitgoyal.chesshelper.feature.overlay.service.OverlayWindowService
import com.rachitgoyal.chesshelper.feature.overlay.service.OverlayWindowServiceState
import com.rachitgoyal.chesshelper.ui.theme.ChessHelperTheme
import com.rachitgoyal.chesshelper.settings.AppSettings
import com.rachitgoyal.chesshelper.ui.app.ChessHelperApp

class MainActivity : ComponentActivity() {
    private var overlayPermissionGranted by mutableStateOf(false)
    private var pendingOverlayLaunch by mutableStateOf(false)
    private var pendingResumeMatchId: String? = null
    private var pendingResumeFromMoveIndex: Int? = null
    private lateinit var appSettings: AppSettings
    private lateinit var matchHistoryRepository: MatchHistoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appSettings = AppSettings(applicationContext)
        matchHistoryRepository = MatchHistoryRepository(applicationContext)
        refreshOverlayPermission()
        setContent {
            ChessHelperTheme {
                ChessHelperApp(
                    appSettings = appSettings,
                    matchHistoryRepository = matchHistoryRepository,
                    overlayPermissionGranted = overlayPermissionGranted,
                    overlayRunning = OverlayWindowServiceState.isRunning,
                    onLaunchOverlay = ::launchOverlay,
                    onStopOverlay = ::stopOverlay,
                    onOpenOverlaySettings = ::openOverlaySettings,
                    onResumeMatchInOverlay = ::resumeMatchInOverlay,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val hadPendingLaunch = pendingOverlayLaunch
        val pendingResumeMatchId = pendingResumeMatchId
        val pendingResumeFromMoveIndex = pendingResumeFromMoveIndex
        refreshOverlayPermission()
        if (!overlayPermissionGranted) return

        if (pendingResumeMatchId != null && pendingResumeFromMoveIndex != null) {
            this.pendingResumeMatchId = null
            this.pendingResumeFromMoveIndex = null
            pendingOverlayLaunch = false
            startForegroundService(
                this,
                OverlayWindowService.createResumeIntent(this, pendingResumeMatchId, pendingResumeFromMoveIndex),
            )
            moveTaskToBack(true)
        } else if (hadPendingLaunch) {
            pendingOverlayLaunch = false
            startOverlayServiceAndBackground()
        }
    }

    private fun refreshOverlayPermission() {
        overlayPermissionGranted = Settings.canDrawOverlays(this)
    }

    private fun launchOverlay() {
        pendingResumeMatchId = null
        pendingResumeFromMoveIndex = null
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

    private fun resumeMatchInOverlay(matchId: String, fromMoveIndex: Int) {
        if (!overlayPermissionGranted) {
            pendingOverlayLaunch = false
            pendingResumeMatchId = matchId
            pendingResumeFromMoveIndex = fromMoveIndex
            openOverlaySettings()
            return
        }
        pendingResumeMatchId = null
        pendingResumeFromMoveIndex = null
        startForegroundService(this, OverlayWindowService.createResumeIntent(this, matchId, fromMoveIndex))
        moveTaskToBack(true)
    }
}
