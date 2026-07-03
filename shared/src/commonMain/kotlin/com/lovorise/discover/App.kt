package com.lovorise.discover

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.lovorise.discover.core.designsystem.LovoriseTheme
import com.lovorise.discover.navigation.LovoriseNavHost

/** Shared root of the app: image loading, theme and navigation. */
@OptIn(ExperimentalCoilApi::class)
@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }
    LovoriseTheme {
        LovoriseNavHost()
    }
}
