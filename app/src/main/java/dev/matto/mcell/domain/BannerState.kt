package dev.matto.mcell.domain

enum class BannerColor { Green, Yellow, Red }

data class BannerState(
    val color: BannerColor,
    val title: String,
    val subtitle: String,
)
