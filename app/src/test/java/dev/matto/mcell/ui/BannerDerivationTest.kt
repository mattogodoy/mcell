package dev.matto.mcell.ui

import dev.matto.mcell.domain.BannerColor
import dev.matto.mcell.domain.BlockStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BannerDerivationTest {

    @Test
    fun `offline wins over hayahora`() {
        val (color, _) = deriveBannerLabels(deviceOffline = true, hayahora = BlockStatus.Active)
        assertEquals(BannerColor.Yellow, color)
    }

    @Test
    fun `hayahora active is red`() {
        val (color, _) = deriveBannerLabels(deviceOffline = false, hayahora = BlockStatus.Active)
        assertEquals(BannerColor.Red, color)
    }

    @Test
    fun `hayahora unknown is yellow`() {
        val (color, _) = deriveBannerLabels(deviceOffline = false, hayahora = BlockStatus.Unknown)
        assertEquals(BannerColor.Yellow, color)
    }

    @Test
    fun `hayahora inactive is green`() {
        val (color, _) = deriveBannerLabels(deviceOffline = false, hayahora = BlockStatus.Inactive)
        assertEquals(BannerColor.Green, color)
    }
}
