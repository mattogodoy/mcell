package dev.matto.mcell.data

import dev.matto.mcell.domain.CheckStatus
import dev.matto.mcell.domain.NetworkErrorReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class UrlChecker(private val client: OkHttpClient) {

    suspend fun check(rawUrl: String): CheckStatus = withContext(Dispatchers.IO) {
        val request = try {
            buildRequest(rawUrl, head = true)
        } catch (_: IllegalArgumentException) {
            return@withContext CheckStatus.NetworkError(NetworkErrorReason.Invalid)
        }
        try {
            client.newCall(request).execute().use { resp ->
                val code = resp.code
                if (code == 405 || code == 501) {
                    val getReq = buildRequest(rawUrl, head = false)
                    client.newCall(getReq).execute().use { getResp ->
                        return@withContext mapCode(getResp.code)
                    }
                }
                mapCode(code)
            }
        } catch (e: Throwable) {
            CheckStatus.NetworkError(mapException(e))
        }
    }

    private fun buildRequest(rawUrl: String, head: Boolean): Request {
        val builder = Request.Builder()
            .url(rawUrl)
            .header("Cache-Control", "no-cache, no-store")
            .header("Pragma", "no-cache")
            .cacheControl(CacheControl.FORCE_NETWORK)
        return if (head) builder.head().build() else builder.get().build()
    }

    private fun mapCode(code: Int): CheckStatus =
        if (code in 200..399) CheckStatus.Reachable(code) else CheckStatus.HttpError(code)

    private fun mapException(e: Throwable): NetworkErrorReason = when (e) {
        is SocketTimeoutException -> NetworkErrorReason.Timeout
        is UnknownHostException -> NetworkErrorReason.Dns
        is ConnectException -> NetworkErrorReason.Refused
        is SSLException -> NetworkErrorReason.Tls
        is IllegalArgumentException -> NetworkErrorReason.Invalid
        is IOException -> NetworkErrorReason.Network
        else -> NetworkErrorReason.Network
    }
}
