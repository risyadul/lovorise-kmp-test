package com.lovorise.discover.core.platform

import androidx.compose.runtime.Composable
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@Composable
actual fun rememberShareText(): (String) -> Unit = { text ->
    val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
    if (rootController != null) {
        val activityController = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null,
        )
        rootController.presentViewController(activityController, animated = true, completion = null)
    }
}
