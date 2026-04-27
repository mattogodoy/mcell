package dev.matto.mcell.domain

data class HomeUiState(
    val urls: List<UrlItem>,
    val hayahora: BlockStatus,
    val vpnActive: Boolean,
    val deviceOffline: Boolean,
    val globalRecheckRunning: Boolean,
    val manageDialogVisible: Boolean,
) {
    companion object {
        val Initial = HomeUiState(
            urls = emptyList(),
            hayahora = BlockStatus.Unknown,
            vpnActive = false,
            deviceOffline = false,
            globalRecheckRunning = false,
            manageDialogVisible = false,
        )
    }
}
