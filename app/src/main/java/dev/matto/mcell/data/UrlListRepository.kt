package dev.matto.mcell.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

interface UrlListRepositoryLike {
    val flow: Flow<List<String>>
    suspend fun save(urls: List<String>)
}

class UrlListRepository(private val store: PrefStore) : UrlListRepositoryLike {

    interface PrefStore {
        val flow: Flow<String?>
        suspend fun setRaw(value: String)
    }

    override val flow: Flow<List<String>> = store.flow.map { raw -> decode(raw) }

    override suspend fun save(urls: List<String>) {
        store.setRaw(json.encodeToString(stringList, urls))
    }

    private fun decode(raw: String?): List<String> {
        if (raw == null) return DefaultUrls
        return runCatching { json.decodeFromString(stringList, raw) }
            .getOrDefault(DefaultUrls)
    }

    companion object {
        val DefaultUrls = listOf(
            "https://1.1.1.1",
            "https://raw.githubusercontent.com/github/gitignore/main/README.md",
            "https://vercel.com",
        )

        private val json = Json { ignoreUnknownKeys = true }
        private val stringList = ListSerializer(String.serializer())

        private val Context.urlsDataStore by preferencesDataStore("urls")
        private val URLS_KEY = stringPreferencesKey("urls_json")

        fun fromAndroidContext(context: Context): UrlListRepository {
            val ds: DataStore<Preferences> = context.urlsDataStore
            val store = object : PrefStore {
                override val flow: Flow<String?> = ds.data.map { it[URLS_KEY] }
                override suspend fun setRaw(value: String) {
                    ds.edit { it[URLS_KEY] = value }
                }
            }
            return UrlListRepository(store)
        }
    }
}
