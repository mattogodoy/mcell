package dev.matto.mcell.ui

import dev.matto.mcell.R
import dev.matto.mcell.domain.BannerColor
import dev.matto.mcell.domain.BlockStatus
import dev.matto.mcell.domain.CheckStatus
import dev.matto.mcell.domain.UrlItem

data class BannerLabels(
    val titleResId: Int,
    val subtitleResId: Int,
)

fun deriveBannerLabels(
    deviceOffline: Boolean,
    hayahora: BlockStatus,
    urls: List<UrlItem>,
): Pair<BannerColor, BannerLabels> {
    if (deviceOffline) {
        return BannerColor.Yellow to BannerLabels(R.string.banner_offline, R.string.banner_offline_subtitle)
    }
    val anyNetworkError = urls.any { it.status is CheckStatus.NetworkError }
    if (hayahora == BlockStatus.Active || anyNetworkError) {
        return BannerColor.Red to BannerLabels(R.string.banner_blocks_active, R.string.banner_blocks_active_subtitle)
    }
    if (hayahora == BlockStatus.Unknown) {
        return BannerColor.Yellow to BannerLabels(R.string.banner_unknown, R.string.banner_unknown_subtitle)
    }
    return BannerColor.Green to BannerLabels(R.string.banner_no_blocks, R.string.banner_no_blocks_subtitle)
}
