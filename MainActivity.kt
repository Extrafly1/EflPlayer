package com.example.eflplayer

import android.Manifest
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.TimeUnit

data class Track(
    val title: String,
    val path: String,
    val cover: ByteArray? = null
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var dominantColor by remember { mutableStateOf(Color(0xFF1E1E1E)) }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = dominantColor,
                    secondary = dominantColor,
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    MusicApp(onDominantColorChange = { color ->
                        dominantColor = color
                    })
                }
            }
        }
    }

    @Composable
    fun MusicApp(onDominantColorChange: (Color) -> Unit) {
        var hasPermission by remember { mutableStateOf(false) }
        val tracks = remember { mutableStateListOf<Track>() }
        var currentIndex by remember { mutableStateOf(-1) }
        var isPlaying by remember { mutableStateOf(false) }
        var progress by remember { mutableStateOf(0f) }
        var isFullScreen by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(true) }

        val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            hasPermission = permissions.values.all { it }
            if (hasPermission) {
                tracks.addAll(scanAudioFiles(Environment.getExternalStorageDirectory()))
                isLoading = false
            } else {
                isLoading = false
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

        // Прогресс трека
        LaunchedEffect(currentIndex, isPlaying) {
            while (isPlaying && mediaPlayer.value?.isPlaying == true) {
                progress = mediaPlayer.value?.currentPosition?.toFloat()
                    ?.div(mediaPlayer.value?.duration ?: 1) ?: 0f
                delay(500)
            }
            isPlaying = false
        }

        fun playTrack(index: Int) {
            if (index in tracks.indices) {
                currentIndex = index
                mediaPlayer.value?.release()
                mediaPlayer.value = MediaPlayer().apply {
                    setDataSource(tracks[index].path)
                    prepare()
                    start()
                }
                isPlaying = true

                val cover = tracks[index].cover
                if (cover != null) {
                    val bitmap = BitmapFactory.decodeByteArray(cover, 0, cover.size)
                    extractDominantColor(bitmap)?.let { color ->
                        onDominantColorChange(color)
                    } ?: run {
                        onDominantColorChange(Color(0xFF1E1E1E))
                    }
                } else {
                    onDominantColorChange(Color(0xFF1E1E1E))
                }
            }
        }

        when {
            isLoading -> LoadingScreen()
            else -> Column(modifier = Modifier.fillMaxSize()) {
                if (!isFullScreen) {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(tracks) { track ->
                            TrackItem(track = track) {
                                val index = tracks.indexOf(track)
                                playTrack(index)
                            }
                        }
                    }
                }

                if (currentIndex in tracks.indices) {
                    val track = tracks[currentIndex]
                    val bitmap = track.cover?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                    val trackDominantColor = bitmap?.let { extractDominantColor(it) } ?: Color(0xFF1E1E1E)
                    val trackProgressColor = bitmap?.let { extractContrastColor(it) } ?: Color(0xFFFF4081)

                    MusicPlayer(
                        title = track.title,
                        artist = "Unknown Artist",
                        cover = track.cover,
                        isPlaying = isPlaying,
                        progress = progress,
                        isFullScreen = isFullScreen,
                        dominantColor = trackDominantColor,
                        progressColor = trackProgressColor,
                        onToggleFullScreen = { isFullScreen = !isFullScreen },
                        onPlayPauseClick = {
                            mediaPlayer.value?.let {
                                if (it.isPlaying) it.pause() else it.start()
                                isPlaying = it.isPlaying
                            }
                        },
                        onNextClick = {
                            val nextIndex = (currentIndex + 1) % tracks.size
                            playTrack(nextIndex)
                        },
                        onPrevClick = {
                            val prevIndex = if (currentIndex - 1 < 0) tracks.size - 1 else currentIndex - 1
                            playTrack(prevIndex)
                        },
                        modifier = if (isFullScreen) Modifier.fillMaxSize()
                        else Modifier.wrapContentHeight().fillMaxWidth(),
                        mediaPlayer = mediaPlayer.value!!
                    )
                }
            }
        }
    }

    private fun scanAudioFiles(dir: File): List<Track> {
        val tracks = mutableListOf<Track>()
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                tracks.addAll(scanAudioFiles(file))
            } else if (file.extension.lowercase() in listOf("mp3", "wav", "m4a", "flac")) {
                val cover = try {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(file.absolutePath)
                    mmr.embeddedPicture
                } catch (e: Exception) { null }
                tracks.add(Track(file.nameWithoutExtension, file.absolutePath, cover))
            }
        }
        return tracks
    }

    private fun extractDominantColor(bitmap: android.graphics.Bitmap): Color? {
        return try {
            val palette = Palette.from(bitmap).generate()
            palette.getDominantColor(android.graphics.Color.DKGRAY).let { Color(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractContrastColor(bitmap: android.graphics.Bitmap): Color {
        return try {
            val palette = Palette.from(bitmap).generate()
            val vibrant = palette.getLightVibrantColor(android.graphics.Color.CYAN)
            Color(vibrant)
        } catch (e: Exception) {
            Color(0xFFFF4081)
        }
    }
}

// Расширение для проверки яркости цвета
private fun Color.isLight(): Boolean {
    val brightness = 0.299 * red + 0.587 * green + 0.114 * blue
    return brightness > 0.7
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFFFF4081))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Загрузка музыки...", color = Color.White)
        }
    }
}

@Composable
fun TrackItem(track: Track, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (track.cover != null) {
                val bitmap = BitmapFactory.decodeByteArray(track.cover, 0, track.cover.size)
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Gray, RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Gray, RoundedCornerShape(8.dp))
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(track.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun MusicPlayer(
    title: String,
    artist: String,
    cover: ByteArray? = null,
    isPlaying: Boolean,
    progress: Float,
    isFullScreen: Boolean,
    dominantColor: Color,
    progressColor: Color,
    onToggleFullScreen: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    modifier: Modifier = Modifier,
    mediaPlayer: MediaPlayer
) {
    val contentColor = if (dominantColor.isLight()) Color.Black else Color.White
    var sliderPosition by remember { mutableStateOf(progress) }

    LaunchedEffect(progress) {
        sliderPosition = progress
    }

    fun formatTime(ms: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms.toLong()) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    Card(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = dominantColor)
    ) {
        if (isFullScreen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onToggleFullScreen) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Свернуть",
                            tint = contentColor
                        )
                    }
                }

                if (cover != null) {
                    val bitmap = BitmapFactory.decodeByteArray(cover, 0, cover.size)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Cover",
                        modifier = Modifier.size(300.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .background(Color.Gray, RoundedCornerShape(16.dp))
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, color = contentColor, style = MaterialTheme.typography.titleLarge)
                    Text(artist, color = contentColor.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Slider вместо LinearProgressIndicator
                    Slider(
                        value = sliderPosition,
                        onValueChange = {
                            sliderPosition = it
                        },
                        onValueChangeFinished = {
                            val newPosition = (mediaPlayer.duration * sliderPosition).toInt()
                            mediaPlayer.seekTo(newPosition)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = progressColor,
                            activeTrackColor = progressColor,
                            inactiveTrackColor = Color.DarkGray.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Время трека
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime((mediaPlayer.currentPosition)), color = contentColor, style = MaterialTheme.typography.bodySmall)
                        Text(formatTime((mediaPlayer.duration)), color = contentColor, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevClick) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = contentColor)
                        }
                        IconButton(onClick = onPlayPauseClick) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = contentColor,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        IconButton(onClick = onNextClick) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = contentColor)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.wrapContentHeight().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onToggleFullScreen) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Развернуть",
                            tint = contentColor
                        )
                    }
                }

                if (cover != null) {
                    val bitmap = BitmapFactory.decodeByteArray(cover, 0, cover.size)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Cover",
                        modifier = Modifier.size(200.dp).padding(bottom = 16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.Gray, RoundedCornerShape(16.dp))
                            .padding(bottom = 16.dp)
                    )
                }

                Text(title, color = contentColor, style = MaterialTheme.typography.titleLarge)
                Text(artist, color = contentColor.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        val newPosition = (mediaPlayer.duration * sliderPosition).toInt()
                        mediaPlayer.seekTo(newPosition)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = progressColor,
                        activeTrackColor = progressColor,
                        inactiveTrackColor = Color.DarkGray.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(mediaPlayer.currentPosition), color = contentColor, style = MaterialTheme.typography.bodySmall)
                    Text(formatTime(mediaPlayer.duration), color = contentColor, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevClick) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = contentColor)
                    }
                    IconButton(onClick = onPlayPauseClick) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = contentColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    IconButton(onClick = onNextClick) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = contentColor)
                    }
                }
            }
        }
    }
}
