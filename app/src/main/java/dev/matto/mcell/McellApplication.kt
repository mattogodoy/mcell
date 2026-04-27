package dev.matto.mcell

import android.app.Application

class McellApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}
