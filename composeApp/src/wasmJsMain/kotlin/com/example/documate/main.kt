package com.example.documate

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.example.documate.core.di.AppContainerHolder
import com.example.documate.core.di.WebAppContainer
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    if (!AppContainerHolder.isInitialized) {
        AppContainerHolder.current = WebAppContainer()
    }

    val body = document.body ?: return
    ComposeViewport(body) {
        App()
    }
}
