package com.echoesapp.audiobooks.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.echoesapp.audiobooks.ui.components.MiniPlayer
import com.echoesapp.audiobooks.ui.navigation.EchoesNavHost
import com.echoesapp.audiobooks.ui.navigation.Screen
import com.echoesapp.audiobooks.ui.screens.player.PlayerViewModel

@Composable
fun EchoesApp(
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val playbackState by playerViewModel.playbackState.collectAsState()
    var showFullPlayer by remember { mutableStateOf(false) }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            EchoesNavHost(
                navController = navController,
                onNavigateToPlayer = { showFullPlayer = true },
            )

            // Mini player at bottom when audio is loaded and not on player screen
            AnimatedVisibility(
                visible = playbackState.audiobook != null && !showFullPlayer,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                MiniPlayer(
                    playbackState = playbackState,
                    onPlayPauseClick = { playerViewModel.togglePlayPause() },
                    onClick = { showFullPlayer = true },
                )
            }
        }
    }

    // Full screen player overlay
    if (showFullPlayer) {
        com.echoesapp.audiobooks.ui.screens.player.PlayerScreen(
            onBackClick = { showFullPlayer = false },
        )
    }
}
