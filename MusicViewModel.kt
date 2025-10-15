package com.example.eflplayer

import android.media.MediaPlayer
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {

    var tracks = mutableStateListOf<Track>()
        private set

    var currentIndex by mutableStateOf(-1)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var progress by mutableStateOf(0f)
        private set

    var mediaPlayer: MediaPlayer? = null

    fun setTracks(list: List<Track>) {
        tracks.clear()
        tracks.addAll(list)
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
        val nextIndex = (currentIndex + 1) % tracks.size
        playTrack(nextIndex)
    }

    fun prevTrack() {
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

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }
}
