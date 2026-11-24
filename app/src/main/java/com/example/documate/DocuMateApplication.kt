package com.example.documate

import android.app.Application
import com.example.documate.core.di.AppContainer
import com.example.documate.core.di.DefaultAppContainer

/**
 * Application class responsible for initializing the dependency container.
 */
class DocuMateApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
