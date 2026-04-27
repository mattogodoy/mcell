package dev.matto.mcell.data

import android.util.Log
import dev.matto.mcell.domain.BlockStatus
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
 * downgrade to `Unknown` rather than throw.
 */
class HayahoraRepository(
    private val client: OkHttpClient,
    private val url: String = "https://hayahora.futbol/estado/data.json",
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun fetchStatus(): BlockStatus = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(url)
                .header("Cache-Control", "no-cache, no-store")
                .header("Pragma", "no-cache")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext BlockStatus.Unknown
                val body = resp.body?.string() ?: return@withContext BlockStatus.Unknown
                parseStatus(body)
            }
        }.getOrElse { e ->
            Log.w("mcell.hayahora", "fetchStatus failed", e)
            BlockStatus.Unknown
        }
    }

    private fun parseStatus(body: String): BlockStatus {
        val element: JsonElement = runCatching { json.parseToJsonElement(body) }
            .getOrElse {
                Log.w("mcell.hayahora", "parse failed: $it")
                return BlockStatus.Unknown
            }
        return runCatching {
            val data = element.jsonObject["data"]?.jsonArray
                ?: return BlockStatus.Unknown
            val anyActive = data.any { entry -> entryIsCurrentlyActive(entry) }
            if (anyActive) BlockStatus.Active else BlockStatus.Inactive
        }.getOrElse {
            Log.w("mcell.hayahora", "schema mismatch: $it")
            BlockStatus.Unknown
        }
    }

    /**
     * An entry is currently active if its most recent `stateChanges` element has
     * `state == true`. The latest element is selected by lexicographic timestamp
     * compare — the observed format `YYYY-MM-DD HH:MM:SSZ` (and any ISO-like
     * variant) sorts identically lexicographically and chronologically. Falls back
     * to the last array element when timestamps are missing.
     */
    private fun entryIsCurrentlyActive(entry: JsonElement): Boolean = runCatching {
        val obj = entry.jsonObject
        val changes = obj["stateChanges"]?.jsonArray ?: return@runCatching false
        if (changes.isEmpty()) return@runCatching false
        val latest = changes.maxByOrNull { c ->
            c.jsonObject["timestamp"]?.jsonPrimitive?.content ?: ""
        } ?: changes.last()
        latest.jsonObject["state"]?.jsonPrimitive?.booleanOrNull ?: false
    }.getOrElse { e ->
        Log.w("mcell.hayahora", "entry parse skipped: $e")
        false
    }
}
