package dev.matto.mcell

import android.content.Context
import dev.matto.mcell.data.HayahoraRepository
import dev.matto.mcell.data.UrlChecker
import dev.matto.mcell.data.UrlListRepository
import dev.matto.mcell.platform.NetworkStateDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob())

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val urlListRepository: UrlListRepository = UrlListRepository.fromAndroidContext(context)
    val urlChecker: UrlChecker = UrlChecker(httpClient)
    val hayahoraRepository: HayahoraRepository = HayahoraRepository(httpClient)
    val networkStateDetector: NetworkStateDetector = NetworkStateDetector(context, applicationScope)
}
