package com.lovorise.discover.core.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Lovorise color tokens, sampled from the production app
 * (primary #F33358, ink #101828, surface tint #EAECF0).
 */
object LovoriseColors {
    val Pink = Color(0xFFF33358)
    val PinkBright = Color(0xFFF33371)
    val PinkSoft = Color(0xFFFFE9EE)
    val Coral = Color(0xFFFF7A59)

    val Ink = Color(0xFF101828)
    val Slate = Color(0xFF475467)
    val Muted = Color(0xFF98A2B3)
    val Border = Color(0xFFEAECF0)
    val SurfaceDim = Color(0xFFF4F5F7)
    val Surface = Color(0xFFF9FAFB)
    val White = Color(0xFFFFFFFF)

    val Success = Color(0xFF12B76A)
    val Scrim = Color(0x47101828)
    val MediaScrim = Color(0xB3101828)

    /** Warm brand gradient used for story rings and hero accents. */
    val BrandGradient = Brush.linearGradient(listOf(Pink, PinkBright, Coral))

    /** Muted identity colors used by the production app for initials avatars. */
    val AvatarPalette = listOf(
        Color(0xFF756E5B),
        Color(0xFF494B79),
        Color(0xFF3E6B6B),
        Color(0xFF6B4A66),
        Color(0xFF4A5A6B),
        Color(0xFF7A5C49),
        Color(0xFF515E43),
        Color(0xFF5B4A79),
    )

    fun avatarColor(seed: String): Color =
        AvatarPalette[(seed.hashCode().let { if (it < 0) -it else it }) % AvatarPalette.size]
}
