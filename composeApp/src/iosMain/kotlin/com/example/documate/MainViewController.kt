package com.example.documate

import androidx.compose.ui.window.ComposeUIViewController
import com.example.documate.core.di.AppContainerHolder
import com.example.documate.core.di.IosAppContainer

/**
 * iOS entry point creating a UIViewController hosting the shared Compose Multiplatform application.
 */
fun MainViewController() = ComposeUIViewController(
    configure = {
        if (!AppContainerHolder.isInitialized) {
            AppContainerHolder.current = IosAppContainer()
        }
    }
) {
    App()
}
