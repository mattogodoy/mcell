package dev.matto.mcell.data

import android.util.Log
import dev.matto.mcell.domain.BlockStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Polls https://hayahora.futbol/estado/data.json and decides whether sport-streaming
 * blocks are *currently* active.
 *
 * Observed schema (April 2026):
 * ```
 * {
 *   "lastUpdate": "2026-04-27 09:44:38",
 *   "data": [
 *     {
 *       "ip": "104.16.93.114",
 *       "isp": "DIGI",
 *       "description": "Cloudflare",
 *       "stateChanges": [
 *         {"timestamp": "2026-02-26 09:44:41Z", "state": false},
 *         {"timestamp": "2026-02-28 16:34:43Z", "state": true},
 *         ...
 *       ]
 *     },
 *     ...
 *   ]
 * }
 * ```
 *
 * The dataset is a per-(ip, isp) history of blocking transitions. A block is currently
 * active for an entry when its most-recent `stateChanges` element has `state == true`.
 * The list-level status returned here is `Active` if **any** entry is currently active,
 * else `Inactive`. Schema surprises (missing `data`, no readable timestamps, etc.)
 * downgrade to `Unknown` rather than throw. If the top-level `lastUpdate` is older
 * than [maxStaleness] (or unparseable), we also return `Unknown` to avoid surfacing
 * data that may not reflect reality.
 */
interface HayahoraRepositoryLike {
    suspend fun fetchStatus(): BlockStatus
}

class HayahoraRepository(
    private val client: OkHttpClient,
    private val url: String = "https://hayahora.futbol/estado/data.json",
    private val clock: Clock = Clock.systemUTC(),
    private val maxStaleness: Duration = Duration.ofHours(6),
) : HayahoraRepositoryLike {
    private val json = Json { ignoreUnknownKeys = true }

    private val lastUpdateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)

    override suspend fun fetchStatus(): BlockStatus = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(url)
                .header("Cache-Control", "no-cache, no-store")
                .header("Pragma", "no-cache")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w("mcell.hayahora", "non-2xx response: ${resp.code}")
                    return@withContext BlockStatus.Unknown
                }
                val body = resp.body?.string() ?: run {
                    Log.w("mcell.hayahora", "empty response body")
                    return@withContext BlockStatus.Unknown
                }
                parseStatus(body)
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Throwable) {
            Log.w("mcell.hayahora", "fetchStatus failed", e)
            BlockStatus.Unknown
        }
    }

    private fun parseStatus(body: String): BlockStatus {
        val element: JsonElement = try {
            json.parseToJsonElement(body)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Throwable) {
            Log.w("mcell.hayahora", "parse failed: $e")
            return BlockStatus.Unknown
        }
        return try {
            val root = element.jsonObject
            val lastUpdate = root["lastUpdate"]?.jsonPrimitive?.content
                ?: return BlockStatus.Unknown
            val lastUpdateInstant = try {
                LocalDateTime.parse(lastUpdate, lastUpdateFormatter)
                    .toInstant(ZoneOffset.UTC)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Throwable) {
                Log.w("mcell.hayahora", "lastUpdate parse failed: $e")
                return BlockStatus.Unknown
            }
            val age = Duration.between(lastUpdateInstant, clock.instant())
            if (age > maxStaleness) {
                Log.w("mcell.hayahora", "data stale: lastUpdate=$lastUpdate age=$age")
                return BlockStatus.Unknown
            }
            val data = root["data"]?.jsonArray
                ?: return BlockStatus.Unknown
            val anyActive = data.any { entry -> entryIsCurrentlyActive(entry) }
            if (anyActive) BlockStatus.Active else BlockStatus.Inactive
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Throwable) {
            Log.w("mcell.hayahora", "schema mismatch: $e")
            BlockStatus.Unknown
        }
    }

    /**
     * An entry is currently active if its most recent `stateChanges` element has
     * `state == true`. The latest element is selected by lexicographic timestamp
     * compare — the observed format `YYYY-MM-DD HH:MM:SSZ` (and any ISO-like
     * variant) sorts identically lexicographically and chronologically.
     */
    private fun entryIsCurrentlyActive(entry: JsonElement): Boolean = runCatching {
        val obj = entry.jsonObject
        val changes = obj["stateChanges"]?.jsonArray ?: return@runCatching false
        if (changes.isEmpty()) return@runCatching false
        val latest = changes.maxByOrNull { c ->
            c.jsonObject["timestamp"]?.jsonPrimitive?.content ?: ""
        }!!
        latest.jsonObject["state"]?.jsonPrimitive?.booleanOrNull ?: false
    }.getOrElse { e ->
        Log.w("mcell.hayahora", "entry parse skipped: $e")
        false
    }
}
