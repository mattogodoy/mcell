package dev.matto.mcell.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.matto.mcell.R
import dev.matto.mcell.domain.CheckStatus
import dev.matto.mcell.domain.NetworkErrorReason
import dev.matto.mcell.domain.UrlItem
import dev.matto.mcell.ui.theme.BgElevated
import dev.matto.mcell.ui.theme.BgFocused
import dev.matto.mcell.ui.theme.StatusGreen
import dev.matto.mcell.ui.theme.StatusRed
import dev.matto.mcell.ui.theme.StatusYellow
import dev.matto.mcell.ui.theme.TextMuted
import dev.matto.mcell.ui.theme.TextSecondary
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun UrlRow(
    item: UrlItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) BgFocused else BgElevated)
            .border(if (focused) 2.dp else 0.dp, Color(0xFF4A9EFF), RoundedCornerShape(8.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(status = item.status)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.url, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (focused) {
                    Text(reasonText(item.status), color = TextSecondary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(timestamp(item), color = TextSecondary)
        }
    }
}

@Composable
private fun StatusDot(status: CheckStatus) {
    when (status) {
        is CheckStatus.Loading -> CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        is CheckStatus.Reachable -> Box(Modifier.size(14.dp).clip(CircleShape).background(StatusGreen))
        is CheckStatus.HttpError -> Box(Modifier.size(14.dp).clip(CircleShape).background(StatusYellow))
        is CheckStatus.NetworkError -> Box(Modifier.size(14.dp).clip(CircleShape).background(StatusRed))
    }
}

@Composable
private fun reasonText(status: CheckStatus): String = when (status) {
    is CheckStatus.Loading -> stringResource(R.string.status_loading)
    is CheckStatus.Reachable -> stringResource(R.string.status_reachable, status.httpCode)
    is CheckStatus.HttpError -> stringResource(R.string.status_http_error, status.httpCode)
    is CheckStatus.NetworkError -> stringResource(when (status.reason) {
        NetworkErrorReason.Timeout -> R.string.reason_timeout
        NetworkErrorReason.Dns -> R.string.reason_dns
        NetworkErrorReason.Refused -> R.string.reason_refused
        NetworkErrorReason.Tls -> R.string.reason_tls
        NetworkErrorReason.Network -> R.string.reason_network
        NetworkErrorReason.Invalid -> R.string.reason_invalid
        NetworkErrorReason.Offline -> R.string.reason_offline
    })
}

@Composable
private fun timestamp(item: UrlItem): String {
    val instant = item.lastCheckedAt ?: return stringResource(R.string.last_checked_never)
    val fmt = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    return fmt.format(instant)
}
