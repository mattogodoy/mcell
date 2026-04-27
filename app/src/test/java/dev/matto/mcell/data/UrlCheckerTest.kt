package dev.matto.mcell.data

import dev.matto.mcell.domain.CheckStatus
import dev.matto.mcell.domain.NetworkErrorReason
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class UrlCheckerTest {

    private lateinit var server: MockWebServer
    private lateinit var checker: UrlChecker

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .callTimeout(3, TimeUnit.SECONDS)
            .build()
        checker = UrlChecker(client)
    }

    @After fun tearDown() { server.shutdown() }

    @Test
    fun `200 maps to Reachable`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        val r = checker.check(server.url("/").toString())
        assertEquals(CheckStatus.Reachable(200), r)
    }

    @Test
    fun `404 maps to HttpError`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val r = checker.check(server.url("/").toString())
        assertEquals(CheckStatus.HttpError(404), r)
    }

    @Test
    fun `503 maps to HttpError`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        val r = checker.check(server.url("/").toString())
        assertEquals(CheckStatus.HttpError(503), r)
    }

    @Test
    fun `405 on HEAD then 200 on GET maps to Reachable`() = runTest {
        server.enqueue(MockResponse().setResponseCode(405)) // HEAD
        server.enqueue(MockResponse().setResponseCode(200)) // GET fallback
        val r = checker.check(server.url("/").toString())
        assertEquals(CheckStatus.Reachable(200), r)
    }

    @Test
    fun `connection refused maps to NetworkError(Refused or Network)`() = runTest {
        val url = server.url("/").toString()
        server.shutdown()
        val r = checker.check(url)
        assertEquals(true, r is CheckStatus.NetworkError)
        val reason = (r as CheckStatus.NetworkError).reason
        assertEquals(true, reason == NetworkErrorReason.Refused || reason == NetworkErrorReason.Network)
    }

    @Test
    fun `socket disconnect during read maps to NetworkError`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val r = checker.check(server.url("/").toString())
        assertEquals(true, r is CheckStatus.NetworkError)
    }

    @Test
    fun `unknown host maps to NetworkError(Dns)`() = runTest {
        val r = checker.check("https://this-host-cannot-resolve-7f3a91.invalid")
        assertEquals(CheckStatus.NetworkError(NetworkErrorReason.Dns), r)
    }

    @Test
    fun `malformed URL maps to NetworkError(Invalid)`() = runTest {
        val r = checker.check("not a url at all")
        assertEquals(CheckStatus.NetworkError(NetworkErrorReason.Invalid), r)
    }
}
