package com.example.eflplayer

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

fun extractDominantColor(bitmap: Bitmap): Color? {
    return try {
        val palette = Palette.from(bitmap).generate()
        palette.getDominantColor(android.graphics.Color.DKGRAY).let { Color(it) }
    } catch (e: Exception) {
        null
    }
}

fun extractContrastColor(bitmap: Bitmap): Color {
    return try {
        val palette = Palette.from(bitmap).generate()
        val dominantColor = palette.getDominantColor(android.graphics.Color.DKGRAY)
        val contrastCandidates = listOf(
            palette.getLightVibrantColor(android.graphics.Color.CYAN),
            palette.getVibrantColor(android.graphics.Color.CYAN),
            palette.getDarkVibrantColor(android.graphics.Color.CYAN),
            palette.getLightMutedColor(android.graphics.Color.CYAN),
            palette.getMutedColor(android.graphics.Color.CYAN),
            palette.getDarkMutedColor(android.graphics.Color.CYAN)
        )
        val contrastColor = contrastCandidates.firstOrNull { it != dominantColor }
            ?: android.graphics.Color.CYAN
        Color(contrastColor)
    } catch (e: Exception) {
        Color(0xFFFF4081)
    }
}

fun Color.isLight(): Boolean {
    val brightness = 0.299 * red + 0.587 * green + 0.114 * blue
    return brightness > 0.7
}
