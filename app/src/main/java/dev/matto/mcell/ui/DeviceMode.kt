package dev.matto.mcell.ui

import androidx.compose.runtime.compositionLocalOf

enum class DeviceMode { Tv, Phone }

val LocalDeviceMode = compositionLocalOf<DeviceMode> { error("DeviceMode not provided") }
