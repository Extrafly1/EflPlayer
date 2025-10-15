package com.example.eflplayer

data class Track(
    val title: String,
    val path: String,
    val cover: ByteArray? = null
)
