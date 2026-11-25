package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val OutfitFontFamily = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.SemiBold),
    Font(R.font.outfit, FontWeight.Bold)
)

val PlusJakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium)
)

val CormorantFontFamily = FontFamily(
    Font(R.font.cormorant_garamond, FontWeight.Normal),
    Font(R.font.cormorant_garamond, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond, FontWeight.Bold)
)

val OswaldFontFamily = FontFamily(
    Font(R.font.oswald, FontWeight.Normal),
    Font(R.font.oswald, FontWeight.Medium),
    Font(R.font.oswald, FontWeight.Bold)
)

val SyneFontFamily = FontFamily(
    Font(R.font.syne, FontWeight.Normal),
    Font(R.font.syne, FontWeight.SemiBold),
    Font(R.font.syne, FontWeight.Bold)
)

val SpaceGroteskFontFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val FredokaFontFamily = FontFamily(
    Font(R.font.fredoka, FontWeight.Normal),
    Font(R.font.fredoka, FontWeight.Medium),
    Font(R.font.fredoka, FontWeight.SemiBold),
    Font(R.font.fredoka, FontWeight.Bold)
)

val ShantellSansFontFamily = FontFamily(
    Font(R.font.shantell_sans, FontWeight.Normal),
    Font(R.font.shantell_sans, FontWeight.Medium),
    Font(R.font.shantell_sans, FontWeight.Bold)
)

val BigShouldersFontFamily = FontFamily(
    Font(R.font.big_shoulders_display, FontWeight.Normal),
    Font(R.font.big_shoulders_display, FontWeight.SemiBold),
    Font(R.font.big_shoulders_display, FontWeight.Bold),
    Font(R.font.big_shoulders_display, FontWeight.ExtraBold)
)

val PlayfairFontFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.SemiBold),
    Font(R.font.playfair_display, FontWeight.Bold)
)

val OrbitronFontFamily = FontFamily(
    Font(R.font.orbitron, FontWeight.Normal),
    Font(R.font.orbitron, FontWeight.Medium),
    Font(R.font.orbitron, FontWeight.Bold)
)

// Backdrop 1.1 — 10 New Bundled Fonts
val BebasNeueFontFamily = FontFamily(
    Font(R.font.bebas_neue, FontWeight.Normal)
)

val CinzelFontFamily = FontFamily(
    Font(R.font.cinzel, FontWeight.Normal),
    Font(R.font.cinzel, FontWeight.SemiBold),
    Font(R.font.cinzel, FontWeight.Bold)
)

val InstrumentSerifFontFamily = FontFamily(
    Font(R.font.instrument_serif, FontWeight.Normal)
)

val BodoniModaFontFamily = FontFamily(
    Font(R.font.bodoni_moda, FontWeight.Normal),
    Font(R.font.bodoni_moda, FontWeight.SemiBold),
    Font(R.font.bodoni_moda, FontWeight.Bold)
)

val RighteousFontFamily = FontFamily(
    Font(R.font.righteous, FontWeight.Normal)
)

val SyneMonoFontFamily = FontFamily(
    Font(R.font.syne_mono, FontWeight.Normal)
)

val SilkscreenFontFamily = FontFamily(
    Font(R.font.silkscreen, FontWeight.Normal)
)

val MajorMonoFontFamily = FontFamily(
    Font(R.font.major_mono_display, FontWeight.Normal)
)

val ArchivoBlackFontFamily = FontFamily(
    Font(R.font.archivo_black, FontWeight.Normal)
)

val CaveatFontFamily = FontFamily(
    Font(R.font.caveat, FontWeight.Normal),
    Font(R.font.caveat, FontWeight.SemiBold),
    Font(R.font.caveat, FontWeight.Bold)
)

// Backdrop 1.3 — 10-Font Studio Typography System
val DmSansFontFamily = FontFamily(
    Font(R.font.dm_sans, FontWeight.Normal),
    Font(R.font.dm_sans, FontWeight.Medium),
    Font(R.font.dm_sans, FontWeight.SemiBold),
    Font(R.font.dm_sans, FontWeight.Bold)
)

val InterFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val InstrumentSansFontFamily = FontFamily(
    Font(R.font.instrument_sans, FontWeight.Normal),
    Font(R.font.instrument_sans, FontWeight.Medium),
    Font(R.font.instrument_sans, FontWeight.SemiBold),
    Font(R.font.instrument_sans, FontWeight.Bold)
)

val DotGothicFontFamily = FontFamily(
    Font(R.font.dotgothic16, FontWeight.Normal)
)

val RobotoFontFamily = FontFamily(
    Font(R.font.roboto, FontWeight.Normal),
    Font(R.font.roboto, FontWeight.Medium),
    Font(R.font.roboto, FontWeight.SemiBold),
    Font(R.font.roboto, FontWeight.Bold)
)

val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins, FontWeight.Normal),
    Font(R.font.poppins, FontWeight.Medium),
    Font(R.font.poppins, FontWeight.SemiBold),
    Font(R.font.poppins, FontWeight.Bold)
)

val MontserratFontFamily = FontFamily(
    Font(R.font.montserrat, FontWeight.Normal),
    Font(R.font.montserrat, FontWeight.Medium),
    Font(R.font.montserrat, FontWeight.SemiBold),
    Font(R.font.montserrat, FontWeight.Bold)
)

fun studioDisplayFontFamily(font: com.example.data.model.BackdropStudioFont): FontFamily = when (font) {
    com.example.data.model.BackdropStudioFont.BACKDROP_DEFAULT -> OutfitFontFamily
    com.example.data.model.BackdropStudioFont.GOOGLE_SANS -> DmSansFontFamily
    com.example.data.model.BackdropStudioFont.APPLE_SF_PRO -> InterFontFamily
    com.example.data.model.BackdropStudioFont.GLYPH -> DotGothicFontFamily
    com.example.data.model.BackdropStudioFont.INTER -> InterFontFamily
    com.example.data.model.BackdropStudioFont.ROBOTO -> RobotoFontFamily
    com.example.data.model.BackdropStudioFont.POPPINS -> PoppinsFontFamily
    com.example.data.model.BackdropStudioFont.MONTSERRAT -> MontserratFontFamily
    com.example.data.model.BackdropStudioFont.OUTFIT -> OutfitFontFamily
    com.example.data.model.BackdropStudioFont.SPACE_GROTESK -> SpaceGroteskFontFamily
}

fun studioBodyFontFamily(font: com.example.data.model.BackdropStudioFont): FontFamily = when (font) {
    com.example.data.model.BackdropStudioFont.BACKDROP_DEFAULT -> PlusJakartaSansFontFamily
    com.example.data.model.BackdropStudioFont.GOOGLE_SANS -> DmSansFontFamily
    com.example.data.model.BackdropStudioFont.APPLE_SF_PRO -> InstrumentSansFontFamily
    com.example.data.model.BackdropStudioFont.GLYPH -> DotGothicFontFamily
    com.example.data.model.BackdropStudioFont.INTER -> InterFontFamily
    com.example.data.model.BackdropStudioFont.ROBOTO -> RobotoFontFamily
    com.example.data.model.BackdropStudioFont.POPPINS -> PoppinsFontFamily
    com.example.data.model.BackdropStudioFont.MONTSERRAT -> MontserratFontFamily
    com.example.data.model.BackdropStudioFont.OUTFIT -> OutfitFontFamily
    com.example.data.model.BackdropStudioFont.SPACE_GROTESK -> SpaceGroteskFontFamily
}

fun buildAppTypography(
    scale: Float = 1.0f,
    studioFont: com.example.data.model.BackdropStudioFont = com.example.data.model.BackdropStudioFont.BACKDROP_DEFAULT
): Typography {
    val s = scale.coerceIn(0.85f, 1.25f)
    val displayFamily = studioDisplayFontFamily(studioFont)
    val bodyFamily = studioBodyFontFamily(studioFont)
    val labelMonoFamily = if (studioFont == com.example.data.model.BackdropStudioFont.GLYPH) {
        DotGothicFontFamily
    } else {
        JetBrainsMonoFontFamily
    }
    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (44f * s).sp,
            lineHeight = (50f * s).sp,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (34f * s).sp,
            lineHeight = (40f * s).sp,
            letterSpacing = (-0.25).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (28f * s).sp,
            lineHeight = (34f * s).sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (23f * s).sp,
            lineHeight = (29f * s).sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (19f * s).sp,
            lineHeight = (25f * s).sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (20f * s).sp,
            lineHeight = (26f * s).sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16f * s).sp,
            lineHeight = (22f * s).sp,
            letterSpacing = 0.1.sp
        ),
        titleSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (14f * s).sp,
            lineHeight = (20f * s).sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (16f * s).sp,
            lineHeight = (24f * s).sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14f * s).sp,
            lineHeight = (20f * s).sp,
            letterSpacing = 0.2.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (12f * s).sp,
            lineHeight = (16f * s).sp,
            letterSpacing = 0.25.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14f * s).sp,
            lineHeight = (20f * s).sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12f * s).sp,
            lineHeight = (16f * s).sp,
            letterSpacing = 0.4.sp
        ),
        labelSmall = TextStyle(
            fontFamily = labelMonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11f * s).sp,
            lineHeight = (14f * s).sp,
            letterSpacing = 0.4.sp
        )
    )
}

val Typography = buildAppTypography(1.0f)
