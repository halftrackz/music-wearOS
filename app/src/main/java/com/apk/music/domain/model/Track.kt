package com.apk.music.domain.model

import android.net.Uri

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Long = 0,
    val uri: Uri? = null,
    val artworkUri: Uri? = null
)
