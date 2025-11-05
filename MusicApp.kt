package com.example.eflplayer

import android.Manifest
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
fun MusicApp(viewModel: MusicViewModel, onDominantColorChange: (Color) -> Unit) {
    var hasPermission by remember { mutableStateOf(false) }
    var playerSize by remember { mutableStateOf(PlayerSize.Medium) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.all { it }
        if (hasPermission) {
            viewModel.loadTracksAsync(Environment.getExternalStorageDirectory())
        }
    }

    // Обработчик для кнопки поделиться (неявное намерение)
    val shareTrack = { track: Track ->
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Послушай трек: ${track.title}\nПуть: ${track.path}")
            putExtra(Intent.EXTRA_SUBJECT, "Поделиться треком")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Поделиться треком"))
    }

    // Обработчик для открытия детальной активности (явное намерение)
    val openTrackDetail = { track: Track ->
        val intent = Intent(context, TrackDetailActivity::class.java).apply {
            putExtra("track", track) // Передаем объект Track
            putExtra("message", "Детальная информация о треке") // Передаем дополнительное сообщение
        }
        context.startActivity(intent)
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
            if (playerSize != PlayerSize.Full) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(viewModel.tracks) { track ->
                        TrackItem(
                            track = track,
                            onClick = {
                                val index = viewModel.tracks.indexOf(track)
                                viewModel.playTrack(index)
                            },
                            onShareClick = { track ->
                                // Двойное действие: поделиться и открыть детали
                                shareTrack(track)
                                openTrackDetail(track)
                            }
                        )
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
                    playerSize = playerSize,
                    dominantColor = trackDominantColor,
                    progressColor = trackProgressColor,
                    onTogglePlayerSize = {
                        playerSize = when(playerSize) {
                            PlayerSize.Full -> PlayerSize.Medium
                            PlayerSize.Medium -> PlayerSize.Mini
                            PlayerSize.Mini -> PlayerSize.Full
                        }
                    },
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onNextClick = { viewModel.nextTrack() },
                    onPrevClick = { viewModel.prevTrack() },
                    modifier = if (playerSize == PlayerSize.Full) Modifier.fillMaxSize()
                    else Modifier.wrapContentHeight().fillMaxWidth(),
                    mediaPlayer = viewModel.mediaPlayer!!
                )
            }
        }
    }
}

fun scanAudioFiles(dir: File): List<Track> {
    val tracks = mutableListOf<Track>()

    // список путей, которые нужно пропустить
    val excludedDirs = listOf(
        "/Android", "/DCIM/.thumbnails", "/system", "/data", "/cache",
        "/proc", "/dev", "/acct", "/vendor", "/sys"
    )

    dir.listFiles()?.forEach { file ->
        // пропускаем скрытые и системные каталоги
        if (file.isDirectory) {
            val path = file.absolutePath
            if (excludedDirs.any { path.startsWith(it) || path.contains(it) } || file.name.startsWith(".")) {
                return@forEach
            }
            tracks.addAll(scanAudioFiles(file))
        } else if (file.extension.lowercase() in listOf("mp3", "wav", "m4a", "flac")) {
            if (file.length() > 10 * 1024) {
                val cover = try {
                    val mmr = android.media.MediaMetadataRetriever()
                    mmr.setDataSource(file.absolutePath)
                    mmr.embeddedPicture
                } catch (e: Exception) {
                    null
                }
                tracks.add(Track(file.nameWithoutExtension, file.absolutePath, cover))
            }
        }
    }

    return tracks
}
