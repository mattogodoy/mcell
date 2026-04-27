package dev.matto.mcell.domain

sealed interface CheckStatus {
    data object Loading : CheckStatus
    data class Reachable(val httpCode: Int) : CheckStatus
    data class HttpError(val httpCode: Int) : CheckStatus
    data class NetworkError(val reason: NetworkErrorReason) : CheckStatus
}

enum class NetworkErrorReason {
    Timeout, Dns, Refused, Tls, Network, Invalid, Offline
}
