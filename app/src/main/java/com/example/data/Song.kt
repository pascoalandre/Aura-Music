package com.example.data

data class Song(
    val id: String,          // Unique ID (can be file path, or media ID)
    val title: String,       // Song title
    val artist: String,      // Song artist
    val album: String,       // Album name
    val path: String,        // Path or content URI
    val duration: Long,      // Duration in milliseconds
    val isDemo: Boolean,     // Whether it is a generated demo wave file
    val year: String? = null // Release year
)
