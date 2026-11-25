package com.example.documate.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.PlatformFile
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.ItemArrayLike
import org.w3c.dom.get

class WebPlatformActions : PlatformActions {
    override fun openFile(fileUriOrPath: String, mimeType: String) {
        window.open(fileUriOrPath, "_blank")
    }

    override fun shareFile(fileUriOrPath: String, mimeType: String) {
        // Fallback on web: open / download link
        window.open(fileUriOrPath, "_blank")
    }

    override fun saveFileToDisk(file: PlatformFile, destinationUriOrName: String, onComplete: (Boolean) -> Unit) {
        try {
            val a = document.createElement("a") as HTMLAnchorElement
            a.href = file.path
            a.download = destinationUriOrName
            document.body?.appendChild(a)
            a.click()
            document.body?.removeChild(a)
            onComplete(true)
        } catch (_: Exception) {
            onComplete(false)
        }
    }
}

@Composable
actual fun rememberPlatformFilePicker(
    allowMultiple: Boolean,
    allowedExtensions: List<String>,
    onFilesPicked: (List<DocumentFile>) -> Unit
): () -> Unit {
    return remember(allowMultiple, allowedExtensions) {
        {
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.multiple = allowMultiple
            if (allowedExtensions.isNotEmpty()) {
                input.accept = allowedExtensions.joinToString(",") {
                    if (it.startsWith(".")) it else ".$it"
                }
            }

            input.onchange = {
                val files = input.files
                if (files != null) {
                    val resultList = mutableListOf<DocumentFile>()
                    for (i in 0 until files.length) {
                        val file = files.item(i)
                        if (file != null) {
                            val name = file.name
                            val ext = name.substringAfterLast('.', "")
                            resultList.add(
                                DocumentFile(
                                    id = "web_file_${name}_$i",
                                    name = name,
                                    uriString = name,
                                    sizeBytes = file.size.toLong(),
                                    mimeType = file.type,
                                    extension = ext
                                )
                            )
                        }
                    }
                    onFilesPicked(resultList)
                }
            }

            input.click()
        }
    }
}

@Composable
actual fun rememberPlatformDocumentSaver(
    suggestedName: String,
    mimeType: String,
    onDestinationSelected: (String) -> Unit
): () -> Unit {
    return remember(suggestedName) {
        {
            onDestinationSelected(suggestedName)
        }
    }
}

@Composable
actual fun rememberPlatformActions(): PlatformActions {
    return remember { WebPlatformActions() }
}
