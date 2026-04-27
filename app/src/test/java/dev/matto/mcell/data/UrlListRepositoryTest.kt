package dev.matto.mcell.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlListRepositoryTest {

    private fun fakeStore(initial: String? = null): FakePrefStore = FakePrefStore(MutableStateFlow(initial))

    @Test
    fun `first read returns baked-in defaults when store is empty`() = runTest {
        val repo = UrlListRepository(fakeStore())
        val urls = repo.flow.first()
        assertEquals(UrlListRepository.DefaultUrls, urls)
    }

    @Test
    fun `save persists list as JSON and flow re-emits`() = runTest {
        val store = fakeStore()
        val repo = UrlListRepository(store)
        repo.save(listOf("https://a.example", "https://b.example"))
        val urls = repo.flow.first()
        assertEquals(listOf("https://a.example", "https://b.example"), urls)
        assertTrue(store.lastWritten?.startsWith("[") ?: false)
    }

    @Test
    fun `corrupt JSON falls back to defaults`() = runTest {
        val store = fakeStore("not-json")
        val repo = UrlListRepository(store)
        val urls = repo.flow.first()
        assertEquals(UrlListRepository.DefaultUrls, urls)
    }

    @Test
    fun `empty list is preserved when explicitly saved`() = runTest {
        val repo = UrlListRepository(fakeStore())
        repo.save(emptyList())
        val urls = repo.flow.first()
        assertTrue(urls.isEmpty())
        assertFalse(urls == UrlListRepository.DefaultUrls)
    }
}

private class FakePrefStore(private val backing: MutableStateFlow<String?>) : UrlListRepository.PrefStore {
    var lastWritten: String? = null
    override val flow get() = backing
    override suspend fun setRaw(value: String) {
        lastWritten = value
        backing.value = value
    }
}
