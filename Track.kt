package com.example.eflplayer

import java.io.Serializable

data class Track(
    val title: String,
    val path: String,
    val cover: ByteArray? = null
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Track
        if (title != other.title) return false
        if (path != other.path) return false
        if (cover != null) {
            if (other.cover == null) return false
            if (!cover.contentEquals(other.cover)) return false
        } else if (other.cover != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + (cover?.contentHashCode() ?: 0)
        return result
    }
}
