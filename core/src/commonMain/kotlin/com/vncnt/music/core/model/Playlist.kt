package com.vncnt.music.core.model

data class Playlist(
    val id: String,
    val title: String,
    val subtitle: String,
    val songCount: Int,
    val artistCount: Int,
    val bannerDrawableName: String = "banner_headphones",
    val description: String = "",
    val isSmartCurated: Boolean = false,
    val filterTag: String = ""
)
