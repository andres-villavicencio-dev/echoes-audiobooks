package com.echoesapp.audiobooks.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.echoesapp.audiobooks.domain.model.Chapter
import com.echoesapp.audiobooks.domain.model.PlaybackState
import com.echoesapp.audiobooks.ui.components.ChapterItem
import com.echoesapp.audiobooks.ui.components.PlaybackControls
import com.echoesapp.audiobooks.ui.components.ProgressBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val showChapterSelector by viewModel.showChapterSelector.collectAsState()
    val showSleepTimerPicker by viewModel.showSleepTimerPicker.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Minimize",
                            modifier = Modifier.size(32.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (playbackState.audiobook != null) {
                PlayerContent(
                    playbackState = playbackState,
                    isBuffering = playbackState.isBuffering,
                    onPlayPauseClick = viewModel::togglePlayPause,
                    onPreviousClick = viewModel::skipPrevious,
                    onNextClick = viewModel::skipNext,
                    onSeek = viewModel::seekTo,
                    onSpeedClick = { /* Show speed picker */ },
                    currentSpeed = playbackState.playbackSpeed,
                    onSpeedChange = viewModel::setPlaybackSpeed,
                    onSleepTimerClick = viewModel::toggleSleepTimerPicker,
                    onChapterListClick = viewModel::toggleChapterSelector,
                )
            } else {
                Text(
                    text = "No audiobook playing",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
        }

        // Chapter selector bottom sheet
        if (showChapterSelector && playbackState.audiobook != null) {
            ChapterSelectorSheet(
                chapters = playbackState.audiobook!!.chapters,
                currentChapterId = playbackState.currentChapter?.id,
                onChapterSelect = viewModel::selectChapter,
                onDismiss = viewModel::toggleChapterSelector,
            )
        }

        // Sleep timer picker bottom sheet
        if (showSleepTimerPicker) {
            SleepTimerSheet(
                currentMinutes = playbackState.sleepTimerMinutes,
                onSelect = viewModel::setSleepTimer,
                onDismiss = viewModel::toggleSleepTimerPicker,
            )
        }
    }
}

@Composable
private fun PlayerContent(
    playbackState: PlaybackState,
    isBuffering: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedClick: () -> Unit,
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onSleepTimerClick: () -> Unit,
    onChapterListClick: () -> Unit,
) {
    val audiobook = playbackState.audiobook ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Cover art
        AsyncImage(
            model = audiobook.coverUrl,
            contentDescription = audiobook.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(24.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp)),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Title and chapter
        Text(
            text = audiobook.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = playbackState.currentChapter?.title ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Progress bar
        ProgressBar(
            position = playbackState.position,
            duration = playbackState.duration,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Playback controls with buffering indicator
        Box(
            contentAlignment = Alignment.Center,
        ) {
            PlaybackControls(
                isPlaying = playbackState.isPlaying,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
            )

            // Buffering indicator overlay
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(72.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    strokeWidth = 3.dp,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Secondary controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Speed control
            SpeedButton(
                currentSpeed = currentSpeed,
                onSpeedChange = onSpeedChange,
            )

            // Sleep timer
            IconButton(onClick = onSleepTimerClick) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = "Sleep timer",
                        tint = if (playbackState.sleepTimerMinutes != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        },
                    )
                    if (playbackState.sleepTimerMinutes != null) {
                        Text(
                            text = "${playbackState.sleepTimerMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Chapter list
            IconButton(onClick = onChapterListClick) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Chapters",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SpeedButton(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    val nextSpeed = speeds[(speeds.indexOf(currentSpeed) + 1) % speeds.size]

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onSpeedChange(nextSpeed) },
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${currentSpeed}x",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterSelectorSheet(
    chapters: List<Chapter>,
    currentChapterId: String?,
    onChapterSelect: (Chapter) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Text(
                text = "Chapters",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LazyColumn {
                itemsIndexed(chapters) { index, chapter ->
                    ChapterItem(
                        chapter = chapter,
                        chapterNumber = index + 1,
                        isCurrentChapter = chapter.id == currentChapterId,
                        isCompleted = false,
                        onClick = { onChapterSelect(chapter) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    currentMinutes: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        null to "Off",
        5 to "5 minutes",
        15 to "15 minutes",
        30 to "30 minutes",
        45 to "45 minutes",
        60 to "1 hour",
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Text(
                text = "Sleep Timer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            options.forEach { (minutes, label) ->
                TextButton(
                    onClick = { onSelect(minutes) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        color = if (minutes == currentMinutes) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = if (minutes == currentMinutes) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
