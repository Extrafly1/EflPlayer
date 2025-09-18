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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.io.File


data class Track(
    val title: String,
    val path: String,
    val cover: ByteArray? = null
)

class MainActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentIndex: Int = -1
    private var isPlaying: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
                    MusicApp()
                }
            }
        }
    }

    @Composable
    fun MusicApp() {
        val context = LocalContext.current
        var hasPermission by remember { mutableStateOf(false) }
        val tracks = remember { mutableStateListOf<Track>() }
        var currentIndex by remember { mutableStateOf(-1) }
        var isPlaying by remember { mutableStateOf(false) }
        var progress by remember { mutableStateOf(0f) }

        val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            hasPermission = permissions.values.all { it }
            if (hasPermission) {
                tracks.addAll(scanAudioFiles(Environment.getExternalStorageDirectory()))
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

        // Прогресс
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
            }
        }

        Column {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(tracks) { track ->
                    TrackItem(track = track) {
                        val index = tracks.indexOf(track)
                        playTrack(index)
                    }
                }
            }

            if (currentIndex in tracks.indices) {
                val track = tracks[currentIndex]
                MusicPlayer(
                    title = track.title,
                    artist = "Unknown Artist",
                    cover = track.cover,
                    isPlaying = isPlaying,
                    progress = progress,
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
                    }
                )
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
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null,
                    modifier = Modifier.size(48.dp).background(Color.Gray, RoundedCornerShape(8.dp)))
            } else {
                Box(modifier = Modifier.size(48.dp).background(Color.Gray, RoundedCornerShape(8.dp)))
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
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (cover != null) {
                val bitmap = BitmapFactory.decodeByteArray(cover, 0, cover.size)
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Cover",
                    modifier = Modifier
                        .size(200.dp)
                        .padding(bottom = 16.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.Gray, RoundedCornerShape(16.dp))
                        .padding(bottom = 16.dp)
                )
            }

            Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(artist, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color(0xFFFF4081),
                trackColor = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevClick) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                }
                IconButton(onClick = onPlayPauseClick) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                IconButton(onClick = onNextClick) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                }
            }
        }
    }
}
