package com.lovorise.discover.core.platform

import androidx.compose.runtime.Composable

/**
 * Platform share sheet for plain text — Android intent chooser,
 * iOS UIActivityViewController.
 */
@Composable
expect fun rememberShareText(): (String) -> Unit
