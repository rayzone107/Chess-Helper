package com.rachitgoyal.chesshelper.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rachitgoyal.chesshelper.data.MatchHistoryRepository
import com.rachitgoyal.chesshelper.feature.history.MatchHistoryContent
import com.rachitgoyal.chesshelper.feature.history.MatchReplayContent
import com.rachitgoyal.chesshelper.feature.settings.SettingsContent
import com.rachitgoyal.chesshelper.settings.AppSettings

private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val MATCH_HISTORY = "match_history"
    const val MATCH_REPLAY = "match_replay/{matchId}"
    fun matchReplay(matchId: String) = "match_replay/$matchId"
}

private fun routeTitle(route: String?): String = when {
    route == null -> "Chess Helper"
    route == Routes.HOME -> "Chess Helper"
    route == Routes.SETTINGS -> "Settings"
    route == Routes.MATCH_HISTORY -> "Match History"
    route.startsWith("match_replay") -> "Match Replay"
    else -> "Chess Helper"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessHelperApp(
    appSettings: AppSettings,
    matchHistoryRepository: MatchHistoryRepository,
    overlayPermissionGranted: Boolean,
    overlayRunning: Boolean,
    onLaunchOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onResumeMatchInOverlay: (matchId: String, fromMoveIndex: Int) -> Unit = { _, _ -> },
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isHome = currentRoute == Routes.HOME || currentRoute == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routeTitle(currentRoute)) },
                navigationIcon = {
                    if (!isHome) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                actions = {
                    if (isHome) {
                        IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeContent(
                    overlayPermissionGranted = overlayPermissionGranted,
                    overlayRunning = overlayRunning,
                    onLaunchOverlay = onLaunchOverlay,
                    onStopOverlay = onStopOverlay,
                    onOpenOverlaySettings = onOpenOverlaySettings,
                    onOpenMatchHistory = { navController.navigate(Routes.MATCH_HISTORY) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsContent(appSettings = appSettings)
            }
            composable(Routes.MATCH_HISTORY) {
                MatchHistoryContent(
                    matches = matchHistoryRepository.loadAll(),
                    onMatchSelected = { match ->
                        navController.navigate(Routes.matchReplay(match.id))
                    },
                    onResumeMatch = { match ->
                        onResumeMatchInOverlay(match.id, match.moves.size)
                    },
                )
            }
            composable(
                route = Routes.MATCH_REPLAY,
                arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
            ) { entry ->
                val matchId = entry.arguments?.getString("matchId") ?: return@composable
                val match = matchHistoryRepository.getById(matchId) ?: return@composable
                MatchReplayContent(
                    match = match,
                    onResumeMatch = { id, fromMoveIndex ->
                        onResumeMatchInOverlay(id, fromMoveIndex)
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    overlayPermissionGranted: Boolean,
    overlayRunning: Boolean,
    onLaunchOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenMatchHistory: () -> Unit,
) {
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
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "♟",
                fontSize = 72.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
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
                    if (overlayPermissionGranted) {
                        AssistChip(
                            onClick = {},
                            label = { Text("✓ Overlay permission granted") },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Color(0xFF22C55E),
                            ),
                        )
                    } else {
                        AssistChip(
                            onClick = onOpenOverlaySettings,
                            label = { Text("✗ Overlay permission required") },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Color(0xFFEF4444),
                            ),
                        )
                    }
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

            OutlinedButton(
                onClick = onOpenMatchHistory,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Match history")
            }

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
