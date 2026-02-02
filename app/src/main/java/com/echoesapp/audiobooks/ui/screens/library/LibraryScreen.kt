package com.echoesapp.audiobooks.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.echoesapp.audiobooks.domain.model.Audiobook
import com.echoesapp.audiobooks.domain.model.AudiobookProgress
import com.echoesapp.audiobooks.ui.components.AdBanner
import com.echoesapp.audiobooks.ui.components.AudiobookCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Echoes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            AdBanner(
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                else -> {
                    LibraryContent(
                        continueListening = uiState.continueListening,
                        classics = uiState.classics,
                        aiStories = uiState.aiStories,
                        onBookClick = onBookClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryContent(
    continueListening: List<AudiobookProgress>,
    classics: List<Audiobook>,
    aiStories: List<Audiobook>,
    onBookClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        // Continue Listening Section
        if (continueListening.isNotEmpty()) {
            item {
                LibrarySection(
                    title = "Continue Listening",
                    items = continueListening,
                    onItemClick = onBookClick,
                    keySelector = { it.audiobook.id },
                    itemContent = { progress ->
                        AudiobookCard(
                            audiobook = progress.audiobook,
                            onClick = { onBookClick(progress.audiobook.id) },
                            progress = progress.percentComplete,
                        )
                    },
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Classics Section
        if (classics.isNotEmpty()) {
            item {
                LibrarySection(
                    title = "Classics",
                    items = classics,
                    onItemClick = onBookClick,
                    keySelector = { it.id },
                    itemContent = { audiobook ->
                        AudiobookCard(
                            audiobook = audiobook,
                            onClick = { onBookClick(audiobook.id) },
                        )
                    },
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // AI Stories Section
        if (aiStories.isNotEmpty()) {
            item {
                LibrarySection(
                    title = "AI Stories",
                    items = aiStories,
                    onItemClick = onBookClick,
                    keySelector = { it.id },
                    itemContent = { audiobook ->
                        AudiobookCard(
                            audiobook = audiobook,
                            onClick = { onBookClick(audiobook.id) },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun <T> LibrarySection(
    title: String,
    items: List<T>,
    onItemClick: (String) -> Unit,
    itemContent: @Composable (T) -> Unit,
    keySelector: (T) -> Any,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onBackground,
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = items,
                key = keySelector,
            ) { item ->
                itemContent(item)
            }
        }
    }
}
