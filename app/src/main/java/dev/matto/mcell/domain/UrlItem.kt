package dev.matto.mcell.domain

import java.time.Instant

data class UrlItem(
    val url: String,
    val status: CheckStatus,
    val lastCheckedAt: Instant?,
)
