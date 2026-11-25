package com.example.documate

import android.app.Application
import com.example.documate.core.di.AndroidAppContainer
import com.example.documate.core.di.AppContainer
import com.example.documate.core.di.AppContainerHolder

class DocuMateApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val appContainer = AndroidAppContainer(this)
        container = appContainer
        AppContainerHolder.current = appContainer
    }
}
