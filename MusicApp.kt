package com.example.eflplayer

import android.Manifest
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.io.File

@Composable
fun MusicApp(viewModel: MusicViewModel, onDominantColorChange: (Color) -> Unit) {
    var hasPermission by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.all { it }
        if (hasPermission) {
            viewModel.loadTracksAsync(Environment.getExternalStorageDirectory())
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            val permissions = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    when {
        viewModel.isLoading -> LoadingScreen()
        else -> Column(modifier = Modifier.fillMaxSize()) {
            if (!isFullScreen) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(viewModel.tracks) { track ->
                        TrackItem(track) {
                            val index = viewModel.tracks.indexOf(track)
                            viewModel.playTrack(index)
                        }
                    }
                }
            }

            if (viewModel.currentIndex in viewModel.tracks.indices) {
                val track = viewModel.tracks[viewModel.currentIndex]
                val bitmap = track.cover?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                val trackDominantColor = bitmap?.let { extractDominantColor(it) } ?: Color(0xFF1E1E1E)
                val trackProgressColor = bitmap?.let { extractContrastColor(it) } ?: Color(0xFFFF4081)

                onDominantColorChange(trackDominantColor)

                MusicPlayer(
                    title = track.title,
                    artist = "Unknown Artist",
                    cover = track.cover,
                    isPlaying = viewModel.isPlaying,
                    progress = viewModel.progress,
                    isFullScreen = isFullScreen,
                    dominantColor = trackDominantColor,
                    progressColor = trackProgressColor,
                    onToggleFullScreen = { isFullScreen = !isFullScreen },
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onNextClick = { viewModel.nextTrack() },
                    onPrevClick = { viewModel.prevTrack() },
                    modifier = if (isFullScreen) Modifier.fillMaxSize()
                    else Modifier.wrapContentHeight().fillMaxWidth(),
                    mediaPlayer = viewModel.mediaPlayer!!
                )
            }
        }
    }
}

fun scanAudioFiles(dir: File): List<Track> {
    val tracks = mutableListOf<Track>()
    dir.listFiles()?.forEach { file ->
        if (file.isDirectory) {
            tracks.addAll(scanAudioFiles(file))
        } else if (file.extension.lowercase() in listOf("mp3", "wav", "m4a", "flac")) {
            // Ограничение по размеру > 10 КБ
            if (file.length() > 10 * 1024) {
                val cover = try {
                    val mmr = android.media.MediaMetadataRetriever()
                    mmr.setDataSource(file.absolutePath)
                    mmr.embeddedPicture
                } catch (e: Exception) { null }
                tracks.add(Track(file.nameWithoutExtension, file.absolutePath, cover))
            }
        }
    }
    return tracks
}

