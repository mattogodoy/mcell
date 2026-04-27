package dev.matto.mcell.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.matto.mcell.R
import dev.matto.mcell.ui.HomeViewModel
import dev.matto.mcell.ui.manage.ManageUrlsDialog

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusBanner(
                deviceOffline = state.deviceOffline,
                hayahora = state.hayahora,
                vpnActive = state.vpnActive,
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(state.urls, key = { it.url }) { item ->
                    UrlRow(item = item, onClick = { viewModel.recheckOne(item.url) })
                }
            }

            Button(
                onClick = { viewModel.recheckAll() },
                enabled = !state.globalRecheckRunning,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.recheck_all)) }

            OutlinedButton(
                onClick = { viewModel.showManageDialog() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.manage_urls)) }
        }

        if (state.manageDialogVisible) {
            ManageUrlsDialog(
                urls = state.urls.map { it.url },
                onAdd = { raw -> viewModel.addUrl(raw) },
                onRemove = { url -> viewModel.removeUrl(url) },
                onDismiss = { viewModel.hideManageDialog() },
            )
        }

        SplashOverlay()
    }
}
