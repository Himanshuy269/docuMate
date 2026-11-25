package com.example.documate.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.documate.core.domain.model.DocumentFile
import com.example.documate.core.domain.model.PlatformFile
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentInteractionController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject

class IosPlatformActions : PlatformActions {
    override fun openFile(fileUriOrPath: String, mimeType: String) {
        val rootVc = getRootViewController() ?: return
        val url = if (fileUriOrPath.startsWith("file://")) {
            NSURL.URLWithString(fileUriOrPath)
        } else {
            NSURL.fileURLWithPath(fileUriOrPath)
        } ?: return

        val docController = UIDocumentInteractionController.interactionControllerWithURL(url)
        docController.presentPreviewAnimated(true)
    }

    override fun shareFile(fileUriOrPath: String, mimeType: String) {
        val rootVc = getRootViewController() ?: return
        val url = if (fileUriOrPath.startsWith("file://")) {
            NSURL.URLWithString(fileUriOrPath)
        } else {
            NSURL.fileURLWithPath(fileUriOrPath)
        } ?: return

        val activityVc = UIActivityViewController(
            activityItems = listOf(url),
            applicationActivities = null
        )
        rootVc.presentViewController(activityVc, animated = true, completion = null)
    }

    override fun saveFileToDisk(file: PlatformFile, destinationUriOrName: String, onComplete: (Boolean) -> Unit) {
        onComplete(true)
    }

    private fun getRootViewController(): UIViewController? {
        val window = UIApplication.sharedApplication.keyWindow ?: UIApplication.sharedApplication.windows.firstOrNull() as? platform.UIKit.UIWindow
        return window?.rootViewController
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
            val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (rootVc != null) {
                val docTypes = if (allowedExtensions.any { it.contains("pdf") }) {
                    listOf("com.adobe.pdf", "public.data")
                } else if (allowedExtensions.any { it.contains("jpg") || it.contains("png") }) {
                    listOf("public.image")
                } else {
                    listOf("public.item")
                }

                val picker = UIDocumentPickerViewController(
                    documentTypes = docTypes,
                    inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
                )
                picker.allowsMultipleSelection = allowMultiple
                val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                        val docs = didPickDocumentsAtURLs.filterIsInstance<NSURL>().map { url ->
                            val path = url.path ?: ""
                            val name = url.lastPathComponent ?: "document"
                            val ext = name.substringAfterLast('.', "")
                            DocumentFile(
                                id = path,
                                name = name,
                                uriString = path,
                                sizeBytes = 0L,
                                mimeType = "",
                                extension = ext
                            )
                        }
                        onFilesPicked(docs)
                    }
                }
                picker.delegate = delegate
                rootVc.presentViewController(picker, animated = true, completion = null)
            }
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
    return remember { IosPlatformActions() }
}
