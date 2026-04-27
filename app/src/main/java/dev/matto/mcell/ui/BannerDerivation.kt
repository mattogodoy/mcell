package dev.matto.mcell.ui

import dev.matto.mcell.R
import dev.matto.mcell.domain.BannerColor
import dev.matto.mcell.domain.BlockStatus

data class BannerLabels(
    val titleResId: Int,
    val subtitleResId: Int,
)

fun deriveBannerLabels(
    deviceOffline: Boolean,
    hayahora: BlockStatus,
): Pair<BannerColor, BannerLabels> {
    if (deviceOffline) {
        return BannerColor.Yellow to BannerLabels(R.string.banner_offline, R.string.banner_offline_subtitle)
    }
    return when (hayahora) {
        BlockStatus.Active -> BannerColor.Red to BannerLabels(R.string.banner_blocks_active, R.string.banner_blocks_active_subtitle)
        BlockStatus.Unknown -> BannerColor.Yellow to BannerLabels(R.string.banner_unknown, R.string.banner_unknown_subtitle)
        BlockStatus.Inactive -> BannerColor.Green to BannerLabels(R.string.banner_no_blocks, R.string.banner_no_blocks_subtitle)
    }
}
