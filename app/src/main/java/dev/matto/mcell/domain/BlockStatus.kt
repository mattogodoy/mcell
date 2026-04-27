package dev.matto.mcell.domain

sealed interface BlockStatus {
    data object Active : BlockStatus
    data object Inactive : BlockStatus
    data object Unknown : BlockStatus
}
