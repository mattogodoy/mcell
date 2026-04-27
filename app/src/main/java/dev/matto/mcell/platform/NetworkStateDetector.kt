package dev.matto.mcell.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

class NetworkStateDetector(
    context: Context,
    scope: CoroutineScope,
) {
    private data class Snapshot(val online: Boolean, val vpn: Boolean)

    private val connectivity: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val snapshots: Flow<Snapshot> = callbackFlow {
        fun emitCurrent() {
            val active = connectivity.activeNetwork
            val caps: NetworkCapabilities? = active?.let { connectivity.getNetworkCapabilities(it) }
            val online = active != null && caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val vpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            trySend(Snapshot(online, vpn))
        }

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = emitCurrent()
            override fun onLost(network: Network) = emitCurrent()
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = emitCurrent()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivity.registerDefaultNetworkCallback(cb)
        } else {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivity.registerNetworkCallback(request, cb)
        }
        emitCurrent()
        awaitClose { connectivity.unregisterNetworkCallback(cb) }
    }.shareIn(scope, SharingStarted.WhileSubscribed(5000), replay = 1)

    val online: Flow<Boolean> = snapshots.map { it.online }.distinctUntilChanged()
    val vpnActive: Flow<Boolean> = snapshots.map { it.vpn }.distinctUntilChanged()
}
