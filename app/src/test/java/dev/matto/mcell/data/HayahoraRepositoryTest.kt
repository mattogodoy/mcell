package dev.matto.mcell.data

import dev.matto.mcell.domain.BlockStatus
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class HayahoraRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    /**
     * Fixtures pin `lastUpdate` to 2026-04-27 12:00:00 UTC; this clock is 30 minutes
     * later, comfortably inside the 6-hour staleness threshold.
     */
    private val freshClock: Clock =
        Clock.fixed(Instant.parse("2026-04-27T12:30:00Z"), ZoneOffset.UTC)

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .callTimeout(3, TimeUnit.SECONDS)
            .build()
    }

    @After fun tearDown() { server.shutdown() }

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResourceAsStream(name)!!.bufferedReader().readText()

    @Test
    fun `active fixture maps to BlockStatus_Active`() = runTest {
        server.enqueue(MockResponse().setBody(resource("hayahora_active.json")))
        val repo = HayahoraRepository(client, server.url("/data.json").toString(), freshClock)
        assertEquals(BlockStatus.Active, repo.fetchStatus())
    }

    @Test
    fun `inactive fixture maps to BlockStatus_Inactive`() = runTest {
        server.enqueue(MockResponse().setBody(resource("hayahora_inactive.json")))
        val repo = HayahoraRepository(client, server.url("/data.json").toString(), freshClock)
        assertEquals(BlockStatus.Inactive, repo.fetchStatus())
    }

    @Test
    fun `malformed JSON returns Unknown`() = runTest {
        server.enqueue(MockResponse().setBody(resource("hayahora_malformed.json")))
        val repo = HayahoraRepository(client, server.url("/data.json").toString(), freshClock)
        assertEquals(BlockStatus.Unknown, repo.fetchStatus())
    }

    @Test
    fun `HTTP 500 returns Unknown`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val repo = HayahoraRepository(client, server.url("/data.json").toString(), freshClock)
        assertEquals(BlockStatus.Unknown, repo.fetchStatus())
    }

    @Test
    fun `connection failure returns Unknown`() = runTest {
        server.shutdown()
        val repo = HayahoraRepository(client, server.url("/data.json").toString(), freshClock)
        assertEquals(BlockStatus.Unknown, repo.fetchStatus())
    }

    @Test
    fun `edge cases all resolve to Inactive`() = runTest {
        server.enqueue(MockResponse().setBody(resource("hayahora_edge.json")))
        val repo = HayahoraRepository(client, server.url("/data.json").toString(), freshClock)
        assertEquals(BlockStatus.Inactive, repo.fetchStatus())
    }

    @Test
    fun `stale lastUpdate returns Unknown`() = runTest {
        server.enqueue(MockResponse().setBody(resource("hayahora_inactive.json")))
        val staleClock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC)
        val repo = HayahoraRepository(client, server.url("/data.json").toString(), staleClock)
        assertEquals(BlockStatus.Unknown, repo.fetchStatus())
    }
}
