package dev.matto.mcell.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.matto.mcell.data.HayahoraRepositoryLike
import dev.matto.mcell.data.UrlCheckerLike
import dev.matto.mcell.data.UrlListRepositoryLike
import dev.matto.mcell.domain.BlockStatus
import dev.matto.mcell.domain.CheckStatus
import dev.matto.mcell.domain.HomeUiState
import dev.matto.mcell.domain.NetworkErrorReason
import dev.matto.mcell.domain.UrlItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URI
import java.time.Instant

enum class AddUrlResult { Added, Duplicate, Full, Invalid }

class HomeViewModel(
    private val listRepo: UrlListRepositoryLike,
    private val checker: UrlCheckerLike,
    private val hayahora: HayahoraRepositoryLike,
    private val online: Flow<Boolean>,
    vpn: Flow<Boolean>,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState.Initial)
    val uiState: StateFlow<HomeUiState> = _state.asStateFlow()

    private val checkJobs = mutableMapOf<String, Job>()
    private var hayahoraJob: Job? = null

    init {
        viewModelScope.launch {
            listRepo.flow.collect { urls ->
                _state.update { current ->
                    val byUrl = current.urls.associateBy { it.url }
                    val merged = urls.map { url ->
                        byUrl[url] ?: UrlItem(url, CheckStatus.Loading, null)
                    }
                    current.copy(urls = merged)
                }
                _state.value.urls.filter { it.status is CheckStatus.Loading }
                    .forEach { recheckOneInternal(it.url) }
            }
        }
        viewModelScope.launch { online.collect { o -> _state.update { it.copy(deviceOffline = !o) } } }
        viewModelScope.launch { vpn.collect { v -> _state.update { it.copy(vpnActive = v) } } }

        recheckAll()
    }

    fun recheckAll() {
        checkJobs.values.forEach { it.cancel() }
        checkJobs.clear()
        hayahoraJob?.cancel()

        if (_state.value.deviceOffline) {
            _state.update { current ->
                current.copy(
                    urls = current.urls.map {
                        it.copy(status = CheckStatus.NetworkError(NetworkErrorReason.Offline), lastCheckedAt = Instant.now())
                    },
                    globalRecheckRunning = false,
                )
            }
            return
        }

        _state.update { current ->
            current.copy(
                urls = current.urls.map { it.copy(status = CheckStatus.Loading) },
                globalRecheckRunning = true,
            )
        }

        hayahoraJob = viewModelScope.launch {
            val status = hayahora.fetchStatus()
            _state.update { it.copy(hayahora = status) }
        }
        _state.value.urls.forEach { item -> recheckOneInternal(item.url) }

        viewModelScope.launch {
            checkJobs.values.toList().forEach { it.join() }
            _state.update { it.copy(globalRecheckRunning = false) }
        }
    }

    fun recheckOne(url: String) {
        if (_state.value.deviceOffline) return
        recheckOneInternal(url)
    }

    private fun recheckOneInternal(url: String) {
        checkJobs[url]?.cancel()
        _state.update { current ->
            current.copy(
                urls = current.urls.map {
                    if (it.url == url) it.copy(status = CheckStatus.Loading) else it
                }
            )
        }
        val job = viewModelScope.launch {
            val status = checker.check(url)
            _state.update { current ->
                current.copy(
                    urls = current.urls.map {
                        if (it.url == url) it.copy(status = status, lastCheckedAt = Instant.now()) else it
                    }
                )
            }
        }
        checkJobs[url] = job
    }

    fun refreshHayahora() {
        hayahoraJob?.cancel()
        hayahoraJob = viewModelScope.launch {
            val status = hayahora.fetchStatus()
            _state.update { it.copy(hayahora = status) }
        }
    }

    suspend fun addUrl(raw: String): AddUrlResult {
        val normalized = normalize(raw) ?: return AddUrlResult.Invalid
        val current = _state.value.urls.map { it.url }
        if (current.size >= MAX_URLS) return AddUrlResult.Full
        val newHost = URI(normalized).host?.lowercase() ?: return AddUrlResult.Invalid
        val existingHosts = current.mapNotNull { runCatching { URI(it).host?.lowercase() }.getOrNull() }
        if (newHost in existingHosts) return AddUrlResult.Duplicate
        listRepo.save(current + normalized)
        return AddUrlResult.Added
    }

    suspend fun removeUrl(url: String) {
        val current = _state.value.urls.map { it.url }.toMutableList()
        current.remove(url)
        listRepo.save(current)
    }

    fun showManageDialog() = _state.update { it.copy(manageDialogVisible = true) }
    fun hideManageDialog() = _state.update { it.copy(manageDialogVisible = false) }

    private fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed
                         else "https://$trimmed"
        return runCatching {
            val uri = URI(withScheme)
            if (uri.host.isNullOrBlank()) null else withScheme
        }.getOrNull()
    }

    companion object { const val MAX_URLS = 6 }
}
