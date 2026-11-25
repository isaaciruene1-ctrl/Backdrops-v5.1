package com.example.ui.components

import androidx.compose.ui.unit.IntSize
import com.example.engine.depth.ClockHorizontalAlign

/**
 * Active pointer grip locked on pointer down inside [ClockBoundingOverlay]
 * so a single finger hold continuously resizes or moves without re-targeting.
 */
enum class ClockGripTarget {
    BODY,
    CORNER_TL,
    CORNER_TR,
    CORNER_BL,
    CORNER_BR,
    EDGE_LEFT,
    EDGE_RIGHT,
    EDGE_TOP,
    EDGE_BOTTOM
}

data class ClockBoxRect(
    val leftPx: Float,
    val topPx: Float,
    val widthPx: Float,
    val heightPx: Float
) {
    val rightPx: Float get() = leftPx + widthPx
    val bottomPx: Float get() = topPx + heightPx
    val centerXPx: Float get() = leftPx + widthPx * 0.5f
    val centerYPx: Float get() = topPx + heightPx * 0.5f
}

/**
 * Clock Scaling and Positioning Engine V5 — Sub-Engine A: Anchor Geometry & Boundary Solver.
 * Guarantees locked opposite-corner anchoring and uniform scale clamping when Free Stretch is disabled.
 */
internal object AnchorGeometrySubEngine {
    fun computeBoxRect(
        unscaledWidthPx: Float,
        unscaledHeightPx: Float,
        viewportSize: IntSize,
        config: ClockDesignConfig,
        minMarginPx: Float = 12f
    ): ClockBoxRect {
        val vw = viewportSize.width.coerceAtLeast(1).toFloat()
        val vh = viewportSize.height.coerceAtLeast(1).toFloat()

        val safeBaseW = unscaledWidthPx.coerceAtLeast(120f)
        val safeBaseH = unscaledHeightPx.coerceAtLeast(72f)

        val maxBoxW = (vw - minMarginPx * 2f).coerceAtLeast(140f)
        val maxBoxH = (vh - minMarginPx * 2f).coerceAtLeast(100f)

        val w: Float
        val h: Float
        if (!config.freeStretchEnabled) {
            val uniformScale = ((config.widthScale + config.heightScale) * 0.5f)
                .coerceIn(ClockSpatialMath.MIN_WIDTH_SCALE, ClockSpatialMath.MAX_WIDTH_SCALE)
            val minU = maxOf(88f / safeBaseW, 58f / safeBaseH)
            val maxU = minOf(maxBoxW / safeBaseW, maxBoxH / safeBaseH).coerceAtLeast(minU)
            val clampedU = uniformScale.coerceIn(minU, maxU)
            w = safeBaseW * clampedU
            h = safeBaseH * clampedU
        } else {
            w = (safeBaseW * config.widthScale).coerceIn(88f, maxBoxW)
            h = (safeBaseH * config.heightScale).coerceIn(58f, maxBoxH)
        }

        val rawCenterX = vw * 0.5f + vw * config.offsetXFraction
        val left = (rawCenterX - w * 0.5f).coerceIn(minMarginPx, (vw - w - minMarginPx).coerceAtLeast(minMarginPx))
        val top = (vh * config.placementY).coerceIn(minMarginPx, (vh - h - minMarginPx).coerceAtLeast(minMarginPx))
        return ClockBoxRect(leftPx = left, topPx = top, widthPx = w, heightPx = h)
    }

    private fun resolveUniformCornerDimensions(
        startW: Float,
        startH: Float,
        rawW: Float,
        rawH: Float,
        minBoxW: Float,
        maxBoxW: Float,
        minBoxH: Float,
        maxBoxH: Float,
        availW: Float,
        availH: Float
    ): Pair<Float, Float> {
        val safeStartW = startW.coerceAtLeast(1f)
        val safeStartH = startH.coerceAtLeast(1f)
        val scaleFactor = ((rawW / safeStartW) + (rawH / safeStartH)) * 0.5f
        val minU = maxOf(minBoxW / safeStartW, minBoxH / safeStartH)
        val maxU = minOf(
            minOf(maxBoxW, availW) / safeStartW,
            minOf(maxBoxH, availH) / safeStartH
        ).coerceAtLeast(minU)
        val u = scaleFactor.coerceIn(minU, maxU)
        return (safeStartW * u) to (safeStartH * u)
    }

    fun applyDragDelta(
        target: ClockGripTarget,
        startConfig: ClockDesignConfig,
        startRect: ClockBoxRect,
        totalDx: Float,
        totalDy: Float,
        viewportSize: IntSize,
        minMarginPx: Float = 12f
    ): ClockDesignConfig {
        val vw = viewportSize.width.coerceAtLeast(1).toFloat()
        val vh = viewportSize.height.coerceAtLeast(1).toFloat()

        val effectiveWScale = if (startConfig.freeStretchEnabled) startConfig.widthScale
        else ((startConfig.widthScale + startConfig.heightScale) * 0.5f)
        val effectiveHScale = if (startConfig.freeStretchEnabled) startConfig.heightScale
        else effectiveWScale
        val baseW = (startRect.widthPx / effectiveWScale.coerceAtLeast(0.25f)).coerceAtLeast(100f)
        val baseH = (startRect.heightPx / effectiveHScale.coerceAtLeast(0.25f)).coerceAtLeast(64f)

        val minBoxW = (baseW * ClockSpatialMath.MIN_WIDTH_SCALE).coerceAtLeast(88f)
        val maxBoxW = minOf(baseW * ClockSpatialMath.MAX_WIDTH_SCALE, (vw - minMarginPx * 2f).coerceAtLeast(minBoxW))
        val minBoxH = (baseH * ClockSpatialMath.MIN_HEIGHT_SCALE).coerceAtLeast(58f)
        val maxBoxH = minOf(baseH * ClockSpatialMath.MAX_HEIGHT_SCALE, (vh - minMarginPx * 2f).coerceAtLeast(minBoxH))

        return when (target) {
            ClockGripTarget.BODY -> {
                val maxLeft = (vw - startRect.widthPx - minMarginPx).coerceAtLeast(minMarginPx)
                val maxTop = (vh - startRect.heightPx - minMarginPx).coerceAtLeast(minMarginPx)
                val newLeft = (startRect.leftPx + totalDx).coerceIn(minMarginPx, maxLeft)
                val newTop = (startRect.topPx + totalDy).coerceIn(minMarginPx, maxTop)
                val newCenterX = newLeft + startRect.widthPx * 0.5f
                startConfig.copy(
                    offsetXFraction = ((newCenterX - vw * 0.5f) / vw),
                    placementY = (newTop / vh),
                    alignment = ClockHorizontalAlign.CENTER
                )
            }

            ClockGripTarget.CORNER_BR -> {
                val anchorX = startRect.leftPx
                val anchorY = startRect.topPx
                val availW = (vw - minMarginPx - anchorX).coerceAtLeast(minBoxW)
                val availH = (vh - minMarginPx - anchorY).coerceAtLeast(minBoxH)
                val rawW = (startRect.widthPx + totalDx).coerceIn(minBoxW, minOf(maxBoxW, availW))
                val rawH = (startRect.heightPx + totalDy).coerceIn(minBoxH, minOf(maxBoxH, availH))

                val (newW, newH) = if (!startConfig.freeStretchEnabled) {
                    resolveUniformCornerDimensions(
                        startW = startRect.widthPx,
                        startH = startRect.heightPx,
                        rawW = rawW,
                        rawH = rawH,
                        minBoxW = minBoxW,
                        maxBoxW = maxBoxW,
                        minBoxH = minBoxH,
                        maxBoxH = maxBoxH,
                        availW = availW,
                        availH = availH
                    )
                } else {
                    rawW to rawH
                }

                val newCenterX = anchorX + newW * 0.5f
                val rawWScale = (newW / baseW).coerceIn(ClockSpatialMath.MIN_WIDTH_SCALE, ClockSpatialMath.MAX_WIDTH_SCALE)
                val rawHScale = (newH / baseH).coerceIn(ClockSpatialMath.MIN_HEIGHT_SCALE, ClockSpatialMath.MAX_HEIGHT_SCALE)
                val uniformU = if (!startConfig.freeStretchEnabled) ((rawWScale + rawHScale) * 0.5f) else rawWScale
                startConfig.copy(
                    scale = startConfig.scale,
                    widthScale = if (!startConfig.freeStretchEnabled) uniformU else rawWScale,
                    heightScale = if (!startConfig.freeStretchEnabled) uniformU else rawHScale,
                    offsetXFraction = (newCenterX - vw * 0.5f) / vw,
                    placementY = anchorY / vh,
                    alignment = ClockHorizontalAlign.CENTER
                )
            }

            ClockGripTarget.CORNER_BL -> {
                val anchorX = startRect.rightPx
                val anchorY = startRect.topPx
                val availW = (anchorX - minMarginPx).coerceAtLeast(minBoxW)
                val availH = (vh - minMarginPx - anchorY).coerceAtLeast(minBoxH)
                val rawW = (startRect.widthPx - totalDx).coerceIn(minBoxW, minOf(maxBoxW, availW))
                val rawH = (startRect.heightPx + totalDy).coerceIn(minBoxH, minOf(maxBoxH, availH))

                val (newW, newH) = if (!startConfig.freeStretchEnabled) {
                    resolveUniformCornerDimensions(
                        startW = startRect.widthPx,
                        startH = startRect.heightPx,
                        rawW = rawW,
                        rawH = rawH,
                        minBoxW = minBoxW,
                        maxBoxW = maxBoxW,
                        minBoxH = minBoxH,
                        maxBoxH = maxBoxH,
                        availW = availW,
                        availH = availH
                    )
                } else {
                    rawW to rawH
                }

                val newCenterX = anchorX - newW * 0.5f
                val rawWScale = (newW / baseW).coerceIn(ClockSpatialMath.MIN_WIDTH_SCALE, ClockSpatialMath.MAX_WIDTH_SCALE)
                val rawHScale = (newH / baseH).coerceIn(ClockSpatialMath.MIN_HEIGHT_SCALE, ClockSpatialMath.MAX_HEIGHT_SCALE)
                val uniformU = if (!startConfig.freeStretchEnabled) ((rawWScale + rawHScale) * 0.5f) else rawWScale
                startConfig.copy(
                    scale = startConfig.scale,
                    widthScale = if (!startConfig.freeStretchEnabled) uniformU else rawWScale,
                    heightScale = if (!startConfig.freeStretchEnabled) uniformU else rawHScale,
                    offsetXFraction = (newCenterX - vw * 0.5f) / vw,
                    placementY = anchorY / vh,
                    alignment = ClockHorizontalAlign.CENTER
                )
            }

            ClockGripTarget.CORNER_TR -> {
                val anchorX = startRect.leftPx
                val anchorY = startRect.bottomPx
                val availW = (vw - minMarginPx - anchorX).coerceAtLeast(minBoxW)
                val availH = (anchorY - minMarginPx).coerceAtLeast(minBoxH)
                val rawW = (startRect.widthPx + totalDx).coerceIn(minBoxW, minOf(maxBoxW, availW))
                val rawH = (startRect.heightPx - totalDy).coerceIn(minBoxH, minOf(maxBoxH, availH))

                val (newW, newH) = if (!startConfig.freeStretchEnabled) {
                    resolveUniformCornerDimensions(
                        startW = startRect.widthPx,
                        startH = startRect.heightPx,
                        rawW = rawW,
                        rawH = rawH,
                        minBoxW = minBoxW,
                        maxBoxW = maxBoxW,
                        minBoxH = minBoxH,
                        maxBoxH = maxBoxH,
                        availW = availW,
                        availH = availH
                    )
                } else {
                    rawW to rawH
                }

                val newTop = anchorY - newH
                val newCenterX = anchorX + newW * 0.5f
                val rawWScale = (newW / baseW).coerceIn(ClockSpatialMath.MIN_WIDTH_SCALE, ClockSpatialMath.MAX_WIDTH_SCALE)
                val rawHScale = (newH / baseH).coerceIn(ClockSpatialMath.MIN_HEIGHT_SCALE, ClockSpatialMath.MAX_HEIGHT_SCALE)
                val uniformU = if (!startConfig.freeStretchEnabled) ((rawWScale + rawHScale) * 0.5f) else rawWScale
                startConfig.copy(
                    scale = startConfig.scale,
                    widthScale = if (!startConfig.freeStretchEnabled) uniformU else rawWScale,
                    heightScale = if (!startConfig.freeStretchEnabled) uniformU else rawHScale,
                    offsetXFraction = (newCenterX - vw * 0.5f) / vw,
                    placementY = newTop / vh,
                    alignment = ClockHorizontalAlign.CENTER
                )
            }

            ClockGripTarget.CORNER_TL -> {
                val anchorX = startRect.rightPx
                val anchorY = startRect.bottomPx
                val availW = (anchorX - minMarginPx).coerceAtLeast(minBoxW)
                val availH = (anchorY - minMarginPx).coerceAtLeast(minBoxH)
                val rawW = (startRect.widthPx - totalDx).coerceIn(minBoxW, minOf(maxBoxW, availW))
                val rawH = (startRect.heightPx - totalDy).coerceIn(minBoxH, minOf(maxBoxH, availH))

                val (newW, newH) = if (!startConfig.freeStretchEnabled) {
                    resolveUniformCornerDimensions(
                        startW = startRect.widthPx,
                        startH = startRect.heightPx,
                        rawW = rawW,
                        rawH = rawH,
                        minBoxW = minBoxW,
                        maxBoxW = maxBoxW,
                        minBoxH = minBoxH,
                        maxBoxH = maxBoxH,
                        availW = availW,
                        availH = availH
                    )
                } else {
                    rawW to rawH
                }

                val newTop = anchorY - newH
                val newCenterX = anchorX - newW * 0.5f
                val rawWScale = (newW / baseW).coerceIn(ClockSpatialMath.MIN_WIDTH_SCALE, ClockSpatialMath.MAX_WIDTH_SCALE)
                val rawHScale = (newH / baseH).coerceIn(ClockSpatialMath.MIN_HEIGHT_SCALE, ClockSpatialMath.MAX_HEIGHT_SCALE)
                val uniformU = if (!startConfig.freeStretchEnabled) ((rawWScale + rawHScale) * 0.5f) else rawWScale
                startConfig.copy(
                    scale = startConfig.scale,
                    widthScale = if (!startConfig.freeStretchEnabled) uniformU else rawWScale,
                    heightScale = if (!startConfig.freeStretchEnabled) uniformU else rawHScale,
                    offsetXFraction = (newCenterX - vw * 0.5f) / vw,
                    placementY = newTop / vh,
                    alignment = ClockHorizontalAlign.CENTER
                )
            }

            ClockGripTarget.EDGE_RIGHT -> {
                val anchorX = startRect.leftPx
                val availW = (vw - minMarginPx - anchorX).coerceAtLeast(minBoxW)
                val newW = (startRect.widthPx + totalDx).coerceIn(minBoxW, minOf(maxBoxW, availW))
                val newCenterX = anchorX + newW * 0.5f
                startConfig.copy(
                    widthScale = (newW / baseW).coerceIn(ClockSpatialMath.MIN_WIDTH_SCALE, ClockSpatialMath.MAX_WIDTH_SCALE),
                    offsetXFraction = (newCenterX - vw * 0.5f) / vw,
                    alignment = ClockHorizontalAlign.CENTER
                )
            }

            ClockGripTarget.EDGE_LEFT -> {
                val anchorX = startRect.rightPx
                val availW = (anchorX - minMarginPx).coerceAtLeast(minBoxW)
                val newW = (startRect.widthPx - totalDx).coerceIn(minBoxW, minOf(maxBoxW, availW))
                val newCenterX = anchorX - newW * 0.5f
                startConfig.copy(
                    widthScale = (newW / baseW).coerceIn(ClockSpatialMath.MIN_WIDTH_SCALE, ClockSpatialMath.MAX_WIDTH_SCALE),
                    offsetXFraction = (newCenterX - vw * 0.5f) / vw,
                    alignment = ClockHorizontalAlign.CENTER
                )
            }

            ClockGripTarget.EDGE_BOTTOM -> {
                val anchorY = startRect.topPx
                val availH = (vh - minMarginPx - anchorY).coerceAtLeast(minBoxH)
                val newH = (startRect.heightPx + totalDy).coerceIn(minBoxH, minOf(maxBoxH, availH))
                startConfig.copy(
                    heightScale = (newH / baseH).coerceIn(ClockSpatialMath.MIN_HEIGHT_SCALE, ClockSpatialMath.MAX_HEIGHT_SCALE)
                )
            }

            ClockGripTarget.EDGE_TOP -> {
                val anchorY = startRect.bottomPx
                val availH = (anchorY - minMarginPx).coerceAtLeast(minBoxH)
                val newH = (startRect.heightPx - totalDy).coerceIn(minBoxH, minOf(maxBoxH, availH))
                val newTop = anchorY - newH
                startConfig.copy(
                    heightScale = (newH / baseH).coerceIn(ClockSpatialMath.MIN_HEIGHT_SCALE, ClockSpatialMath.MAX_HEIGHT_SCALE),
                    placementY = newTop / vh
                )
            }
        }
    }
}

/**
 * Clock Scaling and Positioning Engine V5 — Sub-Engine B: Viewport Fit & Tall-to-Horizontal Morph.
 * Handles hysteresis aspect-profile switching and Lock Screen notification headroom compression.
 */
internal object ViewportAdaptiveMorphSubEngine {
    fun targetProfileForRatio(
        ratio: Float,
        currentProfile: ClockAspectProfile
    ): ClockAspectProfile {
        val tallCutoff = if (currentProfile == ClockAspectProfile.TALL_VERTICAL) 0.98f else 0.90f
        val wideCutoff = if (currentProfile == ClockAspectProfile.WIDE_HORIZONTAL) 1.38f else 1.48f
        return when {
            ratio < tallCutoff -> ClockAspectProfile.TALL_VERTICAL
            ratio > wideCutoff -> ClockAspectProfile.WIDE_HORIZONTAL
            else -> ClockAspectProfile.BALANCED
        }
    }

    fun applyAspectAdaptiveResolution(
        updatedConfig: ClockDesignConfig,
        unscaledWidthPx: Float,
        unscaledHeightPx: Float,
        viewportSize: IntSize,
        minMarginPx: Float = 12f
    ): ClockDesignConfig {
        if (!updatedConfig.aspectAdaptiveEnabled) return updatedConfig

        val rect = AnchorGeometrySubEngine.computeBoxRect(
            unscaledWidthPx = unscaledWidthPx,
            unscaledHeightPx = unscaledHeightPx,
            viewportSize = viewportSize,
            config = updatedConfig,
            minMarginPx = minMarginPx
        )
        val boxRatio = (rect.widthPx / rect.heightPx.coerceAtLeast(1f)).coerceIn(0.25f, 4.0f)
        val desiredProfile = targetProfileForRatio(boxRatio, updatedConfig.face.aspectProfile)
        if (desiredProfile == updatedConfig.face.aspectProfile) {
            return updatedConfig
        }

        val nextFace = when (desiredProfile) {
            ClockAspectProfile.WIDE_HORIZONTAL -> when (updatedConfig.face) {
                HyperOsClockFace.HORIZON_SILK,
                HyperOsClockFace.CANYON_MONUMENT,
                HyperOsClockFace.AQUA_MIST_TALL -> HyperOsClockFace.HORIZON_SILK
                HyperOsClockFace.SWISS_GRID_SPLIT,
                HyperOsClockFace.PIXEL_DUAL_DIAL,
                HyperOsClockFace.BRUTALIST_SLAB -> HyperOsClockFace.SWISS_GRID_SPLIT
                HyperOsClockFace.CHRONO_TELEMETRY,
                HyperOsClockFace.KINETIC_MONO,
                HyperOsClockFace.RETRO_PIXEL_MATRIX,
                HyperOsClockFace.GLYPH_GEOMETRIC,
                HyperOsClockFace.GLYPH_SEGMENTED -> HyperOsClockFace.CHRONO_TELEMETRY
                HyperOsClockFace.ROMAN_MONUMENT,
                HyperOsClockFace.EDITORIAL_DUO -> HyperOsClockFace.ROMAN_MONUMENT
                HyperOsClockFace.SOLAR_ARC_GAUGE,
                HyperOsClockFace.COMPASS_CHRONO -> HyperOsClockFace.SOLAR_ARC_GAUGE
                HyperOsClockFace.HYPEROS_EXTRA_LONG,
                HyperOsClockFace.IOS27_MODERATELY_LONG -> HyperOsClockFace.CLASSIC_HORIZON
                else -> HyperOsClockFace.POSTER_SPLIT
            }
            ClockAspectProfile.BALANCED -> when (updatedConfig.face) {
                HyperOsClockFace.LUXE_VOGUE,
                HyperOsClockFace.EDITORIAL_DUO,
                HyperOsClockFace.CALLIGRAPHY_SEAL -> HyperOsClockFace.LUXE_VOGUE
                HyperOsClockFace.TALL_POSTER_1340,
                HyperOsClockFace.MERIDIAN_SPLIT,
                HyperOsClockFace.HYPEROS_EXTRA_LONG -> HyperOsClockFace.IOS27_MODERATELY_LONG
                HyperOsClockFace.IOS27_MODERATELY_LONG -> HyperOsClockFace.IOS27_MODERATELY_LONG
                HyperOsClockFace.ORBITAL_RING_HYBRID,
                HyperOsClockFace.CHRONO_TELEMETRY,
                HyperOsClockFace.ANALOG_MINIMAL -> HyperOsClockFace.ORBITAL_RING_HYBRID
                HyperOsClockFace.GLYPH_GEOMETRIC -> HyperOsClockFace.GLYPH_GEOMETRIC
                HyperOsClockFace.GLYPH_SEGMENTED -> HyperOsClockFace.GLYPH_SEGMENTED
                HyperOsClockFace.COMPASS_CHRONO,
                HyperOsClockFace.RETRO_PIXEL_MATRIX -> HyperOsClockFace.COMPASS_CHRONO
                HyperOsClockFace.HANDWRITTEN_STAMP,
                HyperOsClockFace.DOODLE_PLAY -> HyperOsClockFace.HANDWRITTEN_STAMP
                HyperOsClockFace.POSTER_SPLIT,
                HyperOsClockFace.MEADOW_BLOCK -> HyperOsClockFace.SPIRE_MONOLITH
                else -> HyperOsClockFace.ATELIER_SERIF
            }
            ClockAspectProfile.TALL_VERTICAL -> when (updatedConfig.face) {
                HyperOsClockFace.HORIZON_SILK,
                HyperOsClockFace.CLASSIC_HORIZON -> HyperOsClockFace.HYPEROS_EXTRA_LONG
                HyperOsClockFace.AQUA_MIST_TALL,
                HyperOsClockFace.ORBITAL_RING_HYBRID -> HyperOsClockFace.AQUA_MIST_TALL
                HyperOsClockFace.EDITORIAL_DUO,
                HyperOsClockFace.ROMAN_MONUMENT -> HyperOsClockFace.EDITORIAL_DUO
                HyperOsClockFace.MERIDIAN_SPLIT,
                HyperOsClockFace.BRUTALIST_SLAB -> HyperOsClockFace.MERIDIAN_SPLIT
                HyperOsClockFace.KINETIC_STEP,
                HyperOsClockFace.DOODLE_PLAY -> HyperOsClockFace.KINETIC_STEP
                HyperOsClockFace.ATELIER_SERIF,
                HyperOsClockFace.LUXE_VOGUE -> HyperOsClockFace.CALLIGRAPHY_SEAL
                HyperOsClockFace.IOS27_MODERATELY_LONG,
                HyperOsClockFace.TALL_POSTER_1340 -> HyperOsClockFace.HYPEROS_EXTRA_LONG
                HyperOsClockFace.MEADOW_BLOCK -> HyperOsClockFace.MEADOW_BLOCK
                else -> HyperOsClockFace.MAGAZINE_STACKED
            }
        }

        val averageScale = ((updatedConfig.widthScale + updatedConfig.heightScale) * 0.5f)
            .coerceIn(0.82f, 1.35f)
        val dampedW = if (updatedConfig.freeStretchEnabled) {
            (averageScale * when (desiredProfile) {
                ClockAspectProfile.WIDE_HORIZONTAL -> 1.06f
                ClockAspectProfile.TALL_VERTICAL -> 0.96f
                ClockAspectProfile.BALANCED -> 1.0f
            }).coerceIn(ClockSpatialMath.MIN_WIDTH_SCALE, ClockSpatialMath.MAX_WIDTH_SCALE)
        } else {
            averageScale
        }
        val dampedH = if (updatedConfig.freeStretchEnabled) {
            (averageScale * when (desiredProfile) {
                ClockAspectProfile.WIDE_HORIZONTAL -> 0.96f
                ClockAspectProfile.TALL_VERTICAL -> 1.06f
                ClockAspectProfile.BALANCED -> 1.0f
            }).coerceIn(ClockSpatialMath.MIN_HEIGHT_SCALE, ClockSpatialMath.MAX_HEIGHT_SCALE)
        } else {
            averageScale
        }

        return updatedConfig.copy(
            face = nextFace,
            font = nextFace.recommendedFont,
            widthScale = dampedW,
            heightScale = dampedH
        )
    }

    /**
     * Morphs tall vertical / oversized editorial clocks into compact horizontal geometry
     * smoothly as Lock Screen notifications expand and reduce available upper headroom.
     */
    fun computeNotificationHeadroomMorph(
        config: ClockDesignConfig,
        notificationExpansionFraction: Float,
        clockOccupiesTallBand: Boolean = config.face.aspectProfile == ClockAspectProfile.TALL_VERTICAL ||
            config.face == HyperOsClockFace.HYPEROS_EXTRA_LONG ||
            config.face == HyperOsClockFace.IOS27_MODERATELY_LONG ||
            config.heightScale > 1.28f
    ): ClockDesignConfig {
        val expansion = notificationExpansionFraction.coerceIn(0f, 1f)
        if (expansion <= 0.08f) return config
        if (!clockOccupiesTallBand) {
            val scaleDown = (1f - expansion * 0.16f).coerceIn(0.76f, 1f)
            return config.copy(
                placementY = (config.placementY * (1f - expansion * 0.28f)).coerceAtLeast(0.05f),
                scale = (config.scale * scaleDown).coerceAtLeast(ClockSpatialMath.MIN_SCALE)
            )
        }
        val compactFace = if (expansion > 0.42f) {
            when (config.face) {
                HyperOsClockFace.HYPEROS_EXTRA_LONG,
                HyperOsClockFace.IOS27_MODERATELY_LONG,
                HyperOsClockFace.CANYON_MONUMENT,
                HyperOsClockFace.AQUA_MIST_TALL -> HyperOsClockFace.CLASSIC_HORIZON
                HyperOsClockFace.EDITORIAL_DUO,
                HyperOsClockFace.CALLIGRAPHY_SEAL,
                HyperOsClockFace.LUXE_VOGUE -> HyperOsClockFace.ROMAN_MONUMENT
                HyperOsClockFace.MAGAZINE_STACKED,
                HyperOsClockFace.MERIDIAN_SPLIT,
                HyperOsClockFace.MEADOW_BLOCK -> HyperOsClockFace.POSTER_SPLIT
                HyperOsClockFace.KINETIC_STEP -> HyperOsClockFace.BRUTALIST_SLAB
                else -> if (config.face.aspectProfile == ClockAspectProfile.TALL_VERTICAL) HyperOsClockFace.CLASSIC_HORIZON else config.face
            }
        } else {
            config.face
        }
        val hShrink = (1f - expansion * 0.34f).coerceIn(ClockSpatialMath.MIN_HEIGHT_SCALE, 1f)
        val wExpand = (1f + expansion * 0.10f).coerceIn(1f, ClockSpatialMath.MAX_WIDTH_SCALE)
        return config.copy(
            face = compactFace,
            font = if (compactFace != config.face) compactFace.recommendedFont else config.font,
            placementY = (config.placementY * (1f - expansion * 0.40f)).coerceAtLeast(0.045f),
            scale = (config.scale * (1f - expansion * 0.18f)).coerceAtLeast(ClockSpatialMath.MIN_SCALE),
            widthScale = (config.widthScale * wExpand).coerceIn(ClockSpatialMath.MIN_WIDTH_SCALE, ClockSpatialMath.MAX_WIDTH_SCALE),
            heightScale = (config.heightScale * hShrink).coerceIn(ClockSpatialMath.MIN_HEIGHT_SCALE, ClockSpatialMath.MAX_HEIGHT_SCALE)
        )
    }
}

object ClockSpatialMath {
    const val MIN_WIDTH_SCALE = 0.50f
    const val MAX_WIDTH_SCALE = 2.35f
    const val MIN_HEIGHT_SCALE = 0.50f
    const val MAX_HEIGHT_SCALE = 2.35f
    const val MIN_SCALE = 0.65f
    const val MAX_SCALE = 1.65f

    const val CENTER_THRESHOLD_FRACTION = 0.055f
    const val DEFAULT_PLACEMENT_Y = 0.14f

    fun computeClockBoxRect(
        unscaledWidthPx: Float,
        unscaledHeightPx: Float,
        viewportSize: IntSize,
        config: ClockDesignConfig,
        minMarginPx: Float = 12f
    ): ClockBoxRect = AnchorGeometrySubEngine.computeBoxRect(
        unscaledWidthPx = unscaledWidthPx,
        unscaledHeightPx = unscaledHeightPx,
        viewportSize = viewportSize,
        config = config,
        minMarginPx = minMarginPx
    )

    fun applyDragDelta(
        target: ClockGripTarget,
        startConfig: ClockDesignConfig,
        startRect: ClockBoxRect,
        totalDx: Float,
        totalDy: Float,
        viewportSize: IntSize,
        minMarginPx: Float = 12f
    ): ClockDesignConfig = AnchorGeometrySubEngine.applyDragDelta(
        target = target,
        startConfig = startConfig,
        startRect = startRect,
        totalDx = totalDx,
        totalDy = totalDy,
        viewportSize = viewportSize,
        minMarginPx = minMarginPx
    )

    fun isOffCenterPastThreshold(
        config: ClockDesignConfig,
        centerAnchorY: Float = config.placementY
    ): Boolean {
        val dx = kotlin.math.abs(config.offsetXFraction)
        val dy = kotlin.math.abs(config.placementY - centerAnchorY)
        return dx > CENTER_THRESHOLD_FRACTION || config.alignment != ClockHorizontalAlign.CENTER || dy > 0.22f
    }

    fun isHorizontallyOffCenter(config: ClockDesignConfig): Boolean =
        kotlin.math.abs(config.offsetXFraction) > 0.015f || config.alignment != ClockHorizontalAlign.CENTER

    fun centerClock(config: ClockDesignConfig): ClockDesignConfig = config.copy(
        offsetXFraction = 0f,
        alignment = ClockHorizontalAlign.CENTER
    )

    fun autoSnapToDepth(
        config: ClockDesignConfig,
        targetPlacementY: Float = DEFAULT_PLACEMENT_Y,
        targetAlignment: ClockHorizontalAlign = ClockHorizontalAlign.CENTER
    ): ClockDesignConfig = config.copy(
        offsetXFraction = 0f,
        placementY = targetPlacementY.coerceIn(0.04f, 0.84f),
        alignment = targetAlignment
    )

    fun resetProportions(config: ClockDesignConfig): ClockDesignConfig = config.copy(
        scale = config.baselineScale,
        widthScale = config.baselineWidthScale,
        heightScale = config.baselineHeightScale
    )

    fun targetProfileForRatio(
        ratio: Float,
        currentProfile: ClockAspectProfile
    ): ClockAspectProfile = ViewportAdaptiveMorphSubEngine.targetProfileForRatio(ratio, currentProfile)

    fun applyAspectAdaptiveResolution(
        updatedConfig: ClockDesignConfig,
        unscaledWidthPx: Float,
        unscaledHeightPx: Float,
        viewportSize: IntSize,
        minMarginPx: Float = 12f
    ): ClockDesignConfig = ViewportAdaptiveMorphSubEngine.applyAspectAdaptiveResolution(
        updatedConfig = updatedConfig,
        unscaledWidthPx = unscaledWidthPx,
        unscaledHeightPx = unscaledHeightPx,
        viewportSize = viewportSize,
        minMarginPx = minMarginPx
    )

    fun computeNotificationHeadroomMorph(
        config: ClockDesignConfig,
        notificationExpansionFraction: Float,
        clockOccupiesTallBand: Boolean = config.face.aspectProfile == ClockAspectProfile.TALL_VERTICAL ||
            config.face == HyperOsClockFace.HYPEROS_EXTRA_LONG ||
            config.face == HyperOsClockFace.IOS27_MODERATELY_LONG ||
            config.heightScale > 1.28f
    ): ClockDesignConfig = ViewportAdaptiveMorphSubEngine.computeNotificationHeadroomMorph(
        config = config,
        notificationExpansionFraction = notificationExpansionFraction,
        clockOccupiesTallBand = clockOccupiesTallBand
    )
}
