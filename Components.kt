package com.example.eflplayer

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import android.media.MediaPlayer
import java.util.concurrent.TimeUnit

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
