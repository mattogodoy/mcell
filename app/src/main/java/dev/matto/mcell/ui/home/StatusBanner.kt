package dev.matto.mcell.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.matto.mcell.R
import dev.matto.mcell.domain.BannerColor
import dev.matto.mcell.domain.BlockStatus
import dev.matto.mcell.ui.deriveBannerLabels
import dev.matto.mcell.ui.theme.BannerGreenBg
import dev.matto.mcell.ui.theme.BannerGreenBorder
import dev.matto.mcell.ui.theme.BannerGreenText
import dev.matto.mcell.ui.theme.BannerRedBg
import dev.matto.mcell.ui.theme.BannerRedBorder
import dev.matto.mcell.ui.theme.BannerRedText
import dev.matto.mcell.ui.theme.BannerYellowBg
import dev.matto.mcell.ui.theme.BannerYellowBorder
import dev.matto.mcell.ui.theme.BannerYellowText
import dev.matto.mcell.ui.theme.VpnPillBg
import dev.matto.mcell.ui.theme.VpnPillText

@Composable
fun StatusBanner(
    deviceOffline: Boolean,
    hayahora: BlockStatus,
    vpnActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val (color, labels) = deriveBannerLabels(deviceOffline, hayahora)
    val (bg, border, text) = when (color) {
        BannerColor.Red -> Triple(BannerRedBg, BannerRedBorder, BannerRedText)
        BannerColor.Yellow -> Triple(BannerYellowBg, BannerYellowBorder, BannerYellowText)
        BannerColor.Green -> Triple(BannerGreenBg, BannerGreenBorder, BannerGreenText)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(labels.titleResId), color = text, fontWeight = FontWeight.SemiBold)
                Text(stringResource(labels.subtitleResId), color = text.copy(alpha = 0.8f))
            }
            VpnPill(active = vpnActive)
        }
    }
}

@Composable
private fun VpnPill(active: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(VpnPillBg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            stringResource(if (active) R.string.vpn_pill_yes else R.string.vpn_pill_no),
            color = VpnPillText,
        )
    }
}
