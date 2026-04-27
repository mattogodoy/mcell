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
 * blocks are *currently* active, mirroring the same rule the hayahora.futbol homepage
 * applies to its hero banner.
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
 * Status rule (from the hayahora.futbol homepage script):
 *  - For each entry whose `description == "Cloudflare"`, take the *last* element of its
 *    `stateChanges` array (by array index — entries are stored chronologically). If
 *    `state == true`, count this ISP toward the entry's IP, and remember the IP if it
 *    is one of the two "key Cloudflare DNS-over-HTTPS" addresses (188.114.96.5 /
 *    188.114.97.5).
 *  - The dataset reports "blocked" if **either** more than ten distinct Cloudflare IPs
 *    are currently blocked by more than two ISPs each, **or** both of the two key
 *    Cloudflare IPs are blocked by any ISP.
 *
 * Schema surprises (missing `data`, no readable timestamps, etc.) downgrade to
 * `Unknown` rather than throw. If the top-level `lastUpdate` is older than
 * [maxStaleness] (or unparseable), we also return `Unknown` to avoid surfacing data
 * that may not reflect reality.
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
            if (computeBlocked(data)) BlockStatus.Active else BlockStatus.Inactive
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Throwable) {
            Log.w("mcell.hayahora", "schema mismatch: $e")
            BlockStatus.Unknown
        }
    }

    private fun computeBlocked(data: List<JsonElement>): Boolean {
        val ispsByCloudflareIp = mutableMapOf<String, MutableSet<String>>()
        val specificBlocked = mutableSetOf<String>()

        for (entry in data) {
            val obj = runCatching { entry.jsonObject }.getOrNull() ?: continue
            if (obj["description"]?.jsonPrimitive?.content != "Cloudflare") continue
            val changes = obj["stateChanges"]?.jsonArray ?: continue
            if (changes.isEmpty()) continue
            val last = runCatching { changes.last().jsonObject }.getOrNull() ?: continue
            if (last["state"]?.jsonPrimitive?.booleanOrNull != true) continue
            val ip = obj["ip"]?.jsonPrimitive?.content ?: continue
            val isp = obj["isp"]?.jsonPrimitive?.content ?: continue
            ispsByCloudflareIp.getOrPut(ip) { mutableSetOf() }.add(isp)
            if (ip in SPECIFIC_CLOUDFLARE_IPS) specificBlocked += ip
        }

        val widelyBlockedCount = ispsByCloudflareIp.values.count { it.size > 2 }
        val bothSpecificBlocked = specificBlocked.containsAll(SPECIFIC_CLOUDFLARE_IPS)
        return widelyBlockedCount > 10 || bothSpecificBlocked
    }

    private companion object {
        val SPECIFIC_CLOUDFLARE_IPS = setOf("188.114.96.5", "188.114.97.5")
    }
}
