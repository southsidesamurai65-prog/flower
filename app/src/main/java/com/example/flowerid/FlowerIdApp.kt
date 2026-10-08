package com.example.flowerid

import android.app.Application
import com.example.flowerid.di.AppContainer

class FlowerIdApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
