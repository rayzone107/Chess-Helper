package com.rachitgoyal.chesshelper.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rachitgoyal.chesshelper.feature.settings.SettingsScreen
import com.rachitgoyal.chesshelper.settings.AppSettings

private enum class AppScreen { HOME, SETTINGS }

@Composable
fun ChessHelperApp(
    appSettings: AppSettings,
    overlayPermissionGranted: Boolean,
    overlayRunning: Boolean,
    onLaunchOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
) {
    var screen by remember { mutableStateOf(AppScreen.HOME) }

    when (screen) {
        AppScreen.SETTINGS -> SettingsScreen(
            appSettings = appSettings,
            onBack = { screen = AppScreen.HOME },
        )

        AppScreen.HOME -> HomeScreen(
            overlayPermissionGranted = overlayPermissionGranted,
            overlayRunning = overlayRunning,
            onLaunchOverlay = onLaunchOverlay,
            onStopOverlay = onStopOverlay,
            onOpenOverlaySettings = onOpenOverlaySettings,
            onOpenSettings = { screen = AppScreen.SETTINGS },
        )
    }
}

@Composable
private fun HomeScreen(
    overlayPermissionGranted: Boolean,
    overlayRunning: Boolean,
    onLaunchOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            Color(0xFF0F172A),
                        ),
                    ),
                ),
        ) {
            // Settings icon in the top-right corner
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                Text(text = "⚙", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = "Chess Overlay Assistant",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Grant overlay access once, then launch the chess coach so it floats above other apps while you study.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyLarge,
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = if (overlayPermissionGranted) "Overlay permission granted" else "Overlay permission required",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = if (overlayPermissionGranted) {
                                "You can launch the floating chess board over YouTube, browsers, and other apps."
                            } else {
                                "Android requires special overlay access before the board can appear above other apps. Open settings, enable permission for Chess Helper, then return here."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (overlayRunning) "Overlay status: active" else "Overlay status: stopped",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (overlayRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        if (overlayPermissionGranted) {
                            Button(
                                onClick = onLaunchOverlay,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Open overlay over other apps")
                            }
                            OutlinedButton(
                                onClick = onStopOverlay,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = overlayRunning,
                            ) {
                                Text("Stop overlay")
                            }
                            OutlinedButton(
                                onClick = onOpenOverlaySettings,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Open overlay settings")
                            }
                        } else {
                            Button(
                                onClick = onOpenOverlaySettings,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Grant overlay permission")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = "After launch, the board stays draggable, minimizable, and interactive above other apps. Use the overlay close button or notification action to dismiss it.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
