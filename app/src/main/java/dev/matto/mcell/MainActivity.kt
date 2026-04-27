package dev.matto.mcell

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.matto.mcell.ui.DeviceMode
import dev.matto.mcell.ui.HomeViewModel
import dev.matto.mcell.ui.LocalDeviceMode
import dev.matto.mcell.ui.home.HomeScreen
import dev.matto.mcell.ui.theme.McellTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as McellApplication).container
        val factory = HomeViewModelFactory(container)
        val viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]
        val deviceMode = if (packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK))
            DeviceMode.Tv else DeviceMode.Phone

        setContent {
            McellTheme {
                CompositionLocalProvider(LocalDeviceMode provides deviceMode) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }
}

private class HomeViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(
            listRepo = container.urlListRepository,
            checker = container.urlChecker,
            hayahora = container.hayahoraRepository,
            online = container.networkStateDetector.online,
            vpn = container.networkStateDetector.vpnActive,
        ) as T
    }
}
