package dev.matto.mcell.ui

import dev.matto.mcell.domain.BannerColor
import dev.matto.mcell.domain.BlockStatus
import dev.matto.mcell.domain.CheckStatus
import dev.matto.mcell.domain.NetworkErrorReason
import dev.matto.mcell.domain.UrlItem
import org.junit.Assert.assertEquals
import org.junit.Test

class BannerDerivationTest {

    private fun urls(vararg statuses: CheckStatus): List<UrlItem> =
        statuses.map { UrlItem("https://x", it, null) }

    @Test
    fun `offline wins over everything else`() {
        val (color, _) = deriveBannerLabels(
            deviceOffline = true,
            hayahora = BlockStatus.Active,
            urls = urls(CheckStatus.NetworkError(NetworkErrorReason.Timeout)),
        )
        assertEquals(BannerColor.Yellow, color)
    }

    @Test
    fun `hayahora active is red`() {
        val (color, _) = deriveBannerLabels(false, BlockStatus.Active, urls(CheckStatus.Reachable(200)))
        assertEquals(BannerColor.Red, color)
    }

    @Test
    fun `local network error is red even when hayahora inactive`() {
        val (color, _) = deriveBannerLabels(
            false, BlockStatus.Inactive,
            urls(CheckStatus.Reachable(200), CheckStatus.NetworkError(NetworkErrorReason.Dns)),
        )
        assertEquals(BannerColor.Red, color)
    }

    @Test
    fun `hayahora unknown is yellow when no local errors`() {
        val (color, _) = deriveBannerLabels(false, BlockStatus.Unknown, urls(CheckStatus.Reachable(200)))
        assertEquals(BannerColor.Yellow, color)
    }

    @Test
    fun `everything fine is green`() {
        val (color, _) = deriveBannerLabels(false, BlockStatus.Inactive, urls(CheckStatus.Reachable(200)))
        assertEquals(BannerColor.Green, color)
    }

    @Test
    fun `http error alone does not turn banner red`() {
        // 503 from a server is not the same as a network-level block.
        val (color, _) = deriveBannerLabels(false, BlockStatus.Inactive, urls(CheckStatus.HttpError(503)))
        assertEquals(BannerColor.Green, color)
    }

    @Test
    fun `loading status is treated as not-an-error`() {
        val (color, _) = deriveBannerLabels(false, BlockStatus.Inactive, urls(CheckStatus.Loading))
        assertEquals(BannerColor.Green, color)
    }
}
