package dev.matto.mcell.ui

import dev.matto.mcell.data.HayahoraRepositoryLike
import dev.matto.mcell.data.UrlCheckerLike
import dev.matto.mcell.data.UrlListRepositoryLike
import dev.matto.mcell.domain.BlockStatus
import dev.matto.mcell.domain.CheckStatus
import dev.matto.mcell.domain.NetworkErrorReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun fakeListRepo(initial: List<String> = listOf("https://a.example", "https://b.example")) =
        FakeUrlListRepository(initial)

    private fun fakeChecker(map: Map<String, CheckStatus> = emptyMap()) = FakeUrlChecker(map)
    private fun fakeHayahora(status: BlockStatus = BlockStatus.Inactive) = FakeHayahoraRepository(status)

    @Test
    fun `init triggers a recheck for every URL and a hayahora fetch`() = runTest(dispatcher) {
        val list = fakeListRepo(listOf("https://a.example"))
        val checker = fakeChecker(mapOf("https://a.example" to CheckStatus.Reachable(200)))
        val hay = fakeHayahora(BlockStatus.Active)

        val vm = HomeViewModel(list, checker, hay, online = MutableStateFlow(true), vpn = MutableStateFlow(false))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, checker.callsFor("https://a.example"))
        assertEquals(BlockStatus.Active, state.hayahora)
        assertEquals(CheckStatus.Reachable(200), state.urls.single().status)
    }

    @Test
    fun `recheckOne updates only that URL`() = runTest(dispatcher) {
        val list = fakeListRepo(listOf("https://a.example", "https://b.example"))
        val checker = fakeChecker(mapOf(
            "https://a.example" to CheckStatus.Reachable(200),
            "https://b.example" to CheckStatus.NetworkError(NetworkErrorReason.Timeout),
        ))
        val vm = HomeViewModel(list, checker, fakeHayahora(), MutableStateFlow(true), MutableStateFlow(false))
        advanceUntilIdle()

        checker.set("https://a.example", CheckStatus.HttpError(503))
        vm.recheckOne("https://a.example")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(CheckStatus.HttpError(503), state.urls.first { it.url == "https://a.example" }.status)
        assertEquals(CheckStatus.NetworkError(NetworkErrorReason.Timeout), state.urls.first { it.url == "https://b.example" }.status)
    }

    @Test
    fun `addUrl with no scheme prepends https`() = runTest(dispatcher) {
        val list = fakeListRepo(emptyList())
        val checker = fakeChecker(mapOf("https://example.com" to CheckStatus.Reachable(200)))
        val vm = HomeViewModel(list, checker, fakeHayahora(), MutableStateFlow(true), MutableStateFlow(false))
        advanceUntilIdle()

        val ok = vm.addUrl("example.com")
        advanceUntilIdle()

        assertEquals(AddUrlResult.Added, ok)
        assertEquals(listOf("https://example.com"), list.flow.first())
    }

    @Test
    fun `addUrl rejects duplicates by host`() = runTest(dispatcher) {
        val list = fakeListRepo(listOf("https://example.com"))
        val vm = HomeViewModel(list, fakeChecker(), fakeHayahora(), MutableStateFlow(true), MutableStateFlow(false))
        advanceUntilIdle()

        val r = vm.addUrl("EXAMPLE.com")
        assertEquals(AddUrlResult.Duplicate, r)
    }

    @Test
    fun `addUrl rejects when at MAX_URLS`() = runTest(dispatcher) {
        val list = fakeListRepo((1..HomeViewModel.MAX_URLS).map { "https://h$it.example" })
        val vm = HomeViewModel(list, fakeChecker(), fakeHayahora(), MutableStateFlow(true), MutableStateFlow(false))
        advanceUntilIdle()

        val r = vm.addUrl("https://newone.example")
        assertEquals(AddUrlResult.Full, r)
    }

    @Test
    fun `addUrl rejects malformed input`() = runTest(dispatcher) {
        val list = fakeListRepo(emptyList())
        val vm = HomeViewModel(list, fakeChecker(), fakeHayahora(), MutableStateFlow(true), MutableStateFlow(false))
        advanceUntilIdle()

        assertEquals(AddUrlResult.Invalid, vm.addUrl(""))
        assertEquals(AddUrlResult.Invalid, vm.addUrl("   "))
        assertEquals(AddUrlResult.Invalid, vm.addUrl("https://"))
    }

    @Test
    fun `removeUrl drops the entry`() = runTest(dispatcher) {
        val list = fakeListRepo(listOf("https://a.example", "https://b.example"))
        val vm = HomeViewModel(list, fakeChecker(), fakeHayahora(), MutableStateFlow(true), MutableStateFlow(false))
        advanceUntilIdle()

        vm.removeUrl("https://a.example")
        advanceUntilIdle()

        assertEquals(listOf("https://b.example"), list.flow.first())
    }

    @Test
    fun `offline state forces all rows to NetworkError(Offline) and skips checks`() = runTest(dispatcher) {
        val list = fakeListRepo(listOf("https://a.example"))
        val checker = fakeChecker()
        val online = MutableStateFlow(true)
        val vm = HomeViewModel(list, checker, fakeHayahora(), online, MutableStateFlow(false))
        advanceUntilIdle()

        online.value = false
        advanceUntilIdle()  // let the collector update _state.deviceOffline
        checker.reset()
        vm.recheckAll()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.deviceOffline)
        assertEquals(0, checker.callsFor("https://a.example"))
        assertEquals(CheckStatus.NetworkError(NetworkErrorReason.Offline), state.urls.single().status)
    }

    @Test
    fun `recheckAll cancels in-flight per-URL checks`() = runTest(dispatcher) {
        val list = fakeListRepo(listOf("https://a.example"))
        val checker = fakeChecker(mapOf("https://a.example" to CheckStatus.Reachable(200)))
        val vm = HomeViewModel(list, checker, fakeHayahora(), MutableStateFlow(true), MutableStateFlow(false))
        advanceUntilIdle()

        checker.delayMs = 1000
        vm.recheckOne("https://a.example")
        vm.recheckAll()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.urls.single().status is CheckStatus.Loading)
    }
}

private class FakeUrlListRepository(initial: List<String>) : UrlListRepositoryLike {
    private val state = MutableStateFlow(initial)
    override val flow get() = state
    override suspend fun save(urls: List<String>) { state.value = urls }
}

private class FakeUrlChecker(initial: Map<String, CheckStatus>) : UrlCheckerLike {
    private val map = initial.toMutableMap()
    private var counts = mutableMapOf<String, Int>()
    var delayMs: Long = 0
    fun set(url: String, status: CheckStatus) { map[url] = status }
    fun reset() { counts = mutableMapOf() }
    fun callsFor(url: String) = counts[url] ?: 0
    override suspend fun check(rawUrl: String): CheckStatus {
        if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
        counts[rawUrl] = (counts[rawUrl] ?: 0) + 1
        return map[rawUrl] ?: CheckStatus.Reachable(200)
    }
}

private class FakeHayahoraRepository(private val status: BlockStatus) : HayahoraRepositoryLike {
    override suspend fun fetchStatus(): BlockStatus = status
}
