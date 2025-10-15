package com.example.eflplayer

import android.media.MediaPlayer
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MusicViewModel : ViewModel() {

    var tracks = mutableStateListOf<Track>()
        private set

    var currentIndex by mutableStateOf(-1)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var progress by mutableStateOf(0f)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var mediaPlayer: MediaPlayer? = null

    fun loadTracksAsync(rootDir: File) {
        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            val scannedTracks = scanAudioFiles(rootDir)
            tracks.clear()
            tracks.addAll(scannedTracks)
            isLoading = false
        }
    }

    fun playTrack(index: Int) {
        if (index !in tracks.indices) return
        currentIndex = index
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(tracks[index].path)
            prepare()
            start()
        }
        isPlaying = true
        updateProgress()
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) it.pause() else it.start()
            isPlaying = it.isPlaying
            updateProgress()
        }
    }

    fun nextTrack() {
        if (tracks.isEmpty()) return
        val nextIndex = (currentIndex + 1) % tracks.size
        playTrack(nextIndex)
    }

    fun prevTrack() {
        if (tracks.isEmpty()) return
        val prevIndex = if (currentIndex - 1 < 0) tracks.size - 1 else currentIndex - 1
        playTrack(prevIndex)
    }

    private fun updateProgress() {
        viewModelScope.launch {
            while (isPlaying && mediaPlayer?.isPlaying == true) {
                progress = mediaPlayer?.currentPosition?.toFloat()?.div(mediaPlayer?.duration ?: 1) ?: 0f
                delay(500)
            }
        }
    }

    private fun scanAudioFiles(dir: File): List<Track> {
        val tracks = mutableListOf<Track>()
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                tracks.addAll(scanAudioFiles(file))
            } else if (file.extension.lowercase() in listOf("mp3", "wav", "m4a", "flac")) {
                if (file.length() > 10 * 1024) { // >10KB
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

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }
}
