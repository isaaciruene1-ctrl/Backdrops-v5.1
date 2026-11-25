package com.example.engine.depth

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.LruCache
import androidx.core.graphics.scale
import com.example.engine.crash.ZeroCrashEngine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Depth Effect Engine V5 — Online-First Multimodal Vision + CIE L*a*b* Sub-Pixel Edge Architecture.
 *
 * When internet connectivity is available, Sub-Engine A invokes Cloud Vision (`gemini-3.8-flash`)
 * to isolate pinpoint foreground polygons and snaps boundaries onto sub-pixel CIE L*a*b* & Sobel
 * contours (fusing ML Kit when present). Strictly falls back to the refined offline CIE L*a*b* + ML Kit
 * pipeline only when offline / no data or when cloud inference is unreachable.
 */
class DepthDetectionEngine @JvmOverloads constructor(
    private val onlineBackend: OnlineDepthVisionBackend = OnlineVisionDepthSolver.FirebaseAiOnlineDepthBackend,
    private val connectivityChecker: (Context?) -> Boolean = OnlineVisionDepthSolver::isOnlineNetworkAvailable
) {

    private val cutoutLruCache = LruCache<String, DepthDetectionResult>(2)

    init {
        ZeroCrashEngine.registerMemoryCleanupHook("DepthDetectionEngine") {
            cutoutLruCache.evictAll()
        }
    }

    data class ForegroundExtractionPacket(
        val fullBmp: Bitmap,
        val cw: Int,
        val ch: Int,
        val rawL: FloatArray,
        val rawA: FloatArray,
        val rawB: FloatArray,
        val smL: FloatArray,
        val smA: FloatArray,
        val smB: FloatArray,
        val edgeMag: FloatArray,
        val topBorderStats: SkyModel,
        val saliency: FloatArray,
        val cleanedMask: BooleanArray,
        val tau: Float,
        val maxGeo: Float,
        val usedMlKit: Boolean,
        val onlinePrediction: OnlineVisionDepthPrediction? = null
    )

    data class SkyModel(
        val protoL: FloatArray,
        val protoA: FloatArray,
        val protoB: FloatArray,
        val meanL: Float,
        val meanA: Float,
        val meanB: Float
    )

    private data class CandidateComponent(
        val pixels: IntArray,
        val area: Int,
        val meanSaliency: Float,
        val minY: Int,
        val maxY: Int,
        val minX: Int,
        val maxX: Int,
        val touchesBottom: Boolean,
        val dominanceScore: Float
    )

    private data class ComponentAnalysisResult(
        val mergedMask: BooleanArray
    )

    data class DepthGeometryEval(
        val isEligible: Boolean,
        val category: DepthSubjectCategory,
        val qualityScore: Int,
        val peakYFraction: Float,
        val peakXFraction: Float,
        val coverageFraction: Float,
        val recommendedClockTopFraction: Float,
        val recommendedAlign: ClockHorizontalAlign,
        val clockOcclusionFraction: Float,
        val recommendedWallpaperZoom: Float = 1.0f,
        val recommendedWallpaperPanXFraction: Float = 0f,
        val recommendedWallpaperPanYFraction: Float = 0f,
        val recommendedClockScale: Float = 1.0f
    )

    /**
     * Sub-Engine A: Foreground Detection, ML Kit Mask Fusion & Separation Matting.
     */
    val foregroundExtractionSubEngine = ForegroundExtractionSubEngine()

    /**
     * Sub-Engine B: Occlusion Intelligence & Non-Overlapping Clock Composition.
     */
    val occlusionCompositionSubEngine = OcclusionCompositionSubEngine()

    suspend fun detectAndExtract(
        sourceBitmap: Bitmap,
        titleHint: String = "Wallpaper",
        cacheKey: String? = null,
        preferPortraitPrior: Boolean = false,
        context: Context? = null,
        onStageProgress: ((DepthScanStage) -> Unit)? = null
    ): DepthDetectionResult = withContext(Dispatchers.Default) {
        val isOnline = connectivityChecker(context)
        if (!cacheKey.isNullOrBlank()) {
            cutoutLruCache.get(cacheKey)?.let { cached ->
                val cutoutValid = cached.cutoutBitmap == null || !cached.cutoutBitmap.isRecycled
                val tierMatches = !isOnline || cached.isOnlineEngine
                if (cutoutValid && tierMatches) {
                    return@withContext cached
                } else {
                    cutoutLruCache.remove(cacheKey)
                }
            }
        }
        val startMs = SystemClock.elapsedRealtime()

        val safeSource = ZeroCrashEngine.guardBitmap(sourceBitmap, requireSoftware = true)
            ?: return@withContext flatResult(titleHint, SystemClock.elapsedRealtime() - startMs, 20f, 0f, 0f)

        val fullBmp = prepareBoundedWorkBitmap(safeSource, MAX_CUTOUT_LONG_EDGE)
            ?: return@withContext flatResult(titleHint, SystemClock.elapsedRealtime() - startMs, 20f, 0f, 0f)

        val fullW = fullBmp.width
        val fullH = fullBmp.height
        if (fullW < 32 || fullH < 32) {
            return@withContext flatResult(titleHint, SystemClock.elapsedRealtime() - startMs, 20f, 0f, 0f)
        }

        val onlinePrediction = if (isOnline) {
            onStageProgress?.invoke(DepthScanStage.CLOUD_VISION)
            try {
                onlineBackend.analyzeForeground(fullBmp, titleHint, preferPortraitPrior)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                ZeroCrashEngine.handleCaughtException("OnlineDepthV5", t)
                null
            }
        } else {
            null
        }

        val result = try {
            val packet = foregroundExtractionSubEngine.extractForegroundPacket(
                fullBmp = fullBmp,
                fullW = fullW,
                fullH = fullH,
                preferPortraitPrior = preferPortraitPrior,
                onlinePrediction = onlinePrediction,
                onStageProgress = onStageProgress
            )
            occlusionCompositionSubEngine.synthesizeComposition(
                packet = packet,
                titleHint = titleHint,
                startMs = startMs,
                onStageProgress = onStageProgress
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            ZeroCrashEngine.handleCaughtException("DepthEngineV5", t)
            flatResult(titleHint, SystemClock.elapsedRealtime() - startMs, 22f, 0f, 0f)
        }

        if (!cacheKey.isNullOrBlank() && (result.cutoutBitmap == null || !result.cutoutBitmap.isRecycled)) {
            cutoutLruCache.put(cacheKey, result)
        }
        result
    }

    suspend fun applyManualMaskRefinement(
        sourceBitmap: Bitmap,
        baseCutout: Bitmap?,
        strokes: List<MaskBrushStroke>,
        featherStrength: Float = 0.45f
    ): Bitmap? = withContext(Dispatchers.Default) {
        if (strokes.isEmpty()) return@withContext baseCutout?.takeIf { !it.isRecycled }
        val safeSource = ZeroCrashEngine.guardBitmap(sourceBitmap, requireSoftware = true)
            ?: return@withContext baseCutout?.takeIf { !it.isRecycled }
        val fullBmp = prepareBoundedWorkBitmap(safeSource, MAX_CUTOUT_LONG_EDGE)
            ?: return@withContext baseCutout?.takeIf { !it.isRecycled }
        val w = fullBmp.width
        val h = fullBmp.height
        val count = w * h

        try {
            val srcPixels = IntArray(count)
            fullBmp.getPixels(srcPixels, 0, w, 0, 0, w, h)

            val alpha = FloatArray(count)
            if (baseCutout != null && !baseCutout.isRecycled && baseCutout.width > 0 && baseCutout.height > 0) {
                val alignedBase = if (baseCutout.width == w && baseCutout.height == h) {
                    ensureSoftwareArgb8888(baseCutout)
                } else {
                    ensureSoftwareArgb8888(baseCutout)?.let { soft ->
                        runCatching { soft.scale(w, h) }.getOrNull()
                    }
                }
                if (alignedBase != null && !alignedBase.isRecycled) {
                    val cutoutPixels = IntArray(count)
                    alignedBase.getPixels(cutoutPixels, 0, w, 0, 0, w, h)
                    for (i in 0 until count) {
                        alpha[i] = ((cutoutPixels[i] ushr 24) and 0xFF) / 255f
                    }
                }
            }

            val minDim = min(w, h).toFloat().coerceAtLeast(1f)
            val safeFeather = featherStrength.coerceIn(0.15f, 1.4f)

            for (stroke in strokes) {
                coroutineContext.ensureActive()
                val pts = stroke.normalizedPoints
                if (pts.isEmpty()) continue
                val radiusPx = (stroke.radiusFraction.coerceIn(0.004f, 0.14f) * minDim).coerceAtLeast(1.5f)
                val featherFrac = (stroke.featherFraction * safeFeather).coerceIn(0.05f, 0.90f)
                val innerRadius = radiusPx * (1f - featherFrac)
                val falloffSpan = (radiusPx - innerRadius).coerceAtLeast(1f)
                val isKeep = stroke.mode == MaskBrushMode.KEEP

                fun stampAt(cx: Float, cy: Float) {
                    val x0 = floor(cx - radiusPx).toInt().coerceIn(0, w - 1)
                    val x1 = floor(cx + radiusPx).toInt().coerceIn(0, w - 1)
                    val y0 = floor(cy - radiusPx).toInt().coerceIn(0, h - 1)
                    val y1 = floor(cy + radiusPx).toInt().coerceIn(0, h - 1)
                    val rSq = radiusPx * radiusPx
                    for (y in y0..y1) {
                        val dy = y - cy
                        val row = y * w
                        for (x in x0..x1) {
                            val dx = x - cx
                            val dSq = dx * dx + dy * dy
                            if (dSq > rSq) continue
                            val dist = sqrt(dSq)
                            val t = ((radiusPx - dist) / falloffSpan).coerceIn(0f, 1f)
                            val weight = t * t * (3f - 2f * t)
                            val idx = row + x
                            alpha[idx] = if (isKeep) {
                                max(alpha[idx], weight)
                            } else {
                                min(alpha[idx], 1f - weight)
                            }
                        }
                    }
                }

                if (pts.size == 1) {
                    stampAt(pts[0].first * w, pts[0].second * h)
                } else {
                    for (pIdx in 0 until pts.lastIndex) {
                        val ax = pts[pIdx].first * w
                        val ay = pts[pIdx].second * h
                        val bx = pts[pIdx + 1].first * w
                        val by = pts[pIdx + 1].second * h
                        val segDist = sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay))
                        val steps = max(1, (segDist / (radiusPx * 0.35f)).roundToInt())
                        for (s in 0..steps) {
                            val f = s.toFloat() / steps.toFloat()
                            stampAt(ax + (bx - ax) * f, ay + (by - ay) * f)
                        }
                    }
                }
            }

            val outPixels = IntArray(count)
            for (i in 0 until count) {
                val a = (alpha[i].coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
                outPixels[i] = if (a == 0) 0 else (a shl 24) or (srcPixels[i] and 0x00FFFFFF)
            }
            val refined = ZeroCrashEngine.safeCreateBitmap(w, h)
                ?: return@withContext baseCutout?.takeIf { !it.isRecycled }
            refined.setPixels(outPixels, 0, w, 0, 0, w, h)
            refined
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            ZeroCrashEngine.handleCaughtException("applyManualMaskRefinement", t)
            baseCutout?.takeIf { !it.isRecycled }
        }
    }

    inner class ForegroundExtractionSubEngine {

        suspend fun extractForegroundPacket(
            fullBmp: Bitmap,
            fullW: Int,
            fullH: Int,
            preferPortraitPrior: Boolean,
            onlinePrediction: OnlineVisionDepthPrediction? = null,
            onStageProgress: ((DepthScanStage) -> Unit)? = null
        ): ForegroundExtractionPacket {
            onStageProgress?.invoke(DepthScanStage.COLOR_SAMPLING)

            val targetCw = min(ANALYSIS_WIDTH, fullW).coerceAtLeast(16)
            val targetCh = max(16, (fullH.toLong() * targetCw / fullW).toInt())
            val coarseBmp = if (fullW == targetCw && fullH == targetCh) {
                fullBmp
            } else {
                fullBmp.scale(targetCw, targetCh)
            }

            val cw = coarseBmp.width
            val ch = coarseBmp.height
            val n = cw * ch
            val coarsePixels = IntArray(n)
            coarseBmp.getPixels(coarsePixels, 0, cw, 0, 0, cw, ch)

            coroutineContext.ensureActive()

            // Optional ML Kit Subject Segmentation confidence mask on the bounded analysis image
            val mlMask = runMlKitForegroundMaskSafe(coarseBmp, cw, ch)

            val rawL = FloatArray(n)
            val rawA = FloatArray(n)
            val rawB = FloatArray(n)
            val coarseLum = FloatArray(n)
            LabColorSpace.convertArgbToLabAndLum(coarsePixels, rawL, rawA, rawB, coarseLum)

            val smL = FloatArray(n)
            val smA = FloatArray(n)
            val smB = FloatArray(n)
            val tmp = FloatArray(n)
            LabColorSpace.smoothBinomial3x3(rawL, smL, tmp, cw, ch)
            LabColorSpace.smoothBinomial3x3(rawA, smA, tmp, cw, ch)
            LabColorSpace.smoothBinomial3x3(rawB, smB, tmp, cw, ch)

            coroutineContext.ensureActive()

            val edgeMag = FloatArray(n)
            computeLabSobel(smL, smA, smB, edgeMag, cw, ch)

            onStageProgress?.invoke(DepthScanStage.BARRIER_TRACING)

            val skySeeds = BooleanArray(n)
            val topBorderStats = buildTopSkyModel(smL, smA, smB, edgeMag, skySeeds, cw, ch)

            val geoDist = FloatArray(n) { Float.POSITIVE_INFINITY }
            for (i in 0 until n) {
                if (skySeeds[i]) geoDist[i] = 0f
            }
            runGeodesicBarrierPasses(geoDist, smL, smA, smB, edgeMag, cw, ch)

            coroutineContext.ensureActive()

            val protoDiv = FloatArray(n)
            computeSkyPrototypeDivergence(smL, smA, smB, topBorderStats, protoDiv)

            val maxGeo = percentile(geoDist, 0.988f).coerceAtLeast(1f)
            val maxDiv = percentile(protoDiv, 0.988f).coerceAtLeast(1f)

            val usedMlKit = mlMask != null && hasUsableMaskVariance(mlMask)
            val validOnlinePrior = onlinePrediction?.takeIf { it.isForegroundSubject && it.hasGeometry }
            val cloudPriorMask = validOnlinePrior?.let {
                OnlineVisionDepthSolver.rasterizePriorMask(it, cw, ch)
            }

            // Estimate foreground CIE L*a*b* prototype inside high-confidence cloud core for contour snapping
            var fgSumL = 0f
            var fgSumA = 0f
            var fgSumB = 0f
            var fgCoreCount = 0
            if (cloudPriorMask != null) {
                for (i in 0 until n) {
                    if (cloudPriorMask[i] >= 0.85f) {
                        fgSumL += smL[i]
                        fgSumA += smA[i]
                        fgSumB += smB[i]
                        fgCoreCount++
                    }
                }
            }
            val fgMeanL = if (fgCoreCount > 0) fgSumL / fgCoreCount else 50f
            val fgMeanA = if (fgCoreCount > 0) fgSumA / fgCoreCount else 0f
            val fgMeanB = if (fgCoreCount > 0) fgSumB / fgCoreCount else 0f

            val saliency = FloatArray(n)
            for (i in 0 until n) {
                val gNorm = (geoDist[i] / maxGeo).coerceIn(0f, 1.35f)
                val dNorm = (protoDiv[i] / maxDiv).coerceIn(0f, 1.25f)
                val labFused = (0.82f * gNorm + 0.18f * dNorm).coerceIn(0f, 1f)
                val localBase = if (usedMlKit && mlMask != null) {
                    val mlVal = mlMask[i].coerceIn(0f, 1f)
                    (0.58f * mlVal + 0.42f * labFused).coerceIn(0f, 1f)
                } else {
                    labFused
                }

                val fusedCore = if (cloudPriorMask != null) {
                    val cPrior = cloudPriorMask[i].coerceIn(0f, 1f)
                    when {
                        cPrior >= 0.88f -> (0.76f * cPrior + 0.24f * max(localBase, 0.55f)).coerceIn(0f, 1f)
                        cPrior <= 0.12f -> (0.28f * localBase * (cPrior / 0.12f + 0.35f)).coerceIn(0f, 0.45f)
                        else -> {
                            // Transition collar: snap contours cleanly to CIE L*a*b* affinity + Sobel edges
                            val edgeAffinity = if (fgCoreCount > 0) {
                                val dFg = LabColorSpace.deltaE(smL[i], smA[i], smB[i], fgMeanL, fgMeanA, fgMeanB)
                                val dBg = LabColorSpace.deltaE(
                                    smL[i], smA[i], smB[i],
                                    topBorderStats.meanL, topBorderStats.meanA, topBorderStats.meanB
                                )
                                (dBg / (dFg + dBg + 1f)).coerceIn(0f, 1f)
                            } else {
                                localBase
                            }
                            (0.46f * cPrior + 0.34f * edgeAffinity + 0.20f * localBase).coerceIn(0f, 1f)
                        }
                    }
                } else if (onlinePrediction != null && !onlinePrediction.isForegroundSubject &&
                    onlinePrediction.category == DepthSubjectCategory.FLAT_CANVAS &&
                    onlinePrediction.confidence >= 0.72f
                ) {
                    // Online vision explicitly identified flat canvas with high confidence
                    (localBase * 0.32f).coerceIn(0f, 0.40f)
                } else {
                    localBase
                }

                val portraitBoost = if (preferPortraitPrior) {
                    val py = (i / cw).toFloat() / ch.toFloat()
                    val px = (i % cw).toFloat() / cw.toFloat()
                    val centerDist = abs(px - 0.5f)
                    if (py in 0.20f..0.95f && centerDist < 0.36f) 0.08f else 0f
                } else {
                    0f
                }
                saliency[i] = (fusedCore + portraitBoost).coerceIn(0f, 1f)
            }

            val tau = if (cloudPriorMask != null) {
                computeOtsuThreshold(saliency).coerceIn(0.32f, 0.58f)
            } else {
                computeOtsuThreshold(saliency).coerceIn(0.28f, 0.64f)
            }
            val binaryMask = BooleanArray(n) { i -> saliency[i] >= tau }
            val filtered = filterComponentsAndCloseHoles(binaryMask, saliency, cw, ch).mergedMask
            val cleanedMask = closeSmallInteriorHoles(filtered, cw, ch)

            return ForegroundExtractionPacket(
                fullBmp = fullBmp,
                cw = cw,
                ch = ch,
                rawL = rawL,
                rawA = rawA,
                rawB = rawB,
                smL = smL,
                smA = smA,
                smB = smB,
                edgeMag = edgeMag,
                topBorderStats = topBorderStats,
                saliency = saliency,
                cleanedMask = cleanedMask,
                tau = tau,
                maxGeo = maxGeo,
                usedMlKit = usedMlKit,
                onlinePrediction = onlinePrediction
            )
        }

        private suspend fun runMlKitForegroundMaskSafe(
            bitmap: Bitmap,
            expectedW: Int,
            expectedH: Int
        ): FloatArray? {
            if (bitmap.isRecycled || bitmap.width != expectedW || bitmap.height != expectedH) return null
            return try {
                val options = SubjectSegmenterOptions.Builder()
                    .enableForegroundConfidenceMask()
                    .build()
                val segmenter = SubjectSegmentation.getClient(options)
                val input = InputImage.fromBitmap(bitmap, 0)
                try {
                    val maskBuffer = suspendCancellableCoroutine<FloatBuffer?> { cont ->
                        val task = segmenter.process(input)
                        task.addOnSuccessListener { result ->
                            if (cont.isActive) {
                                cont.resume(result?.foregroundConfidenceMask)
                            }
                        }
                        task.addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                        task.addOnCanceledListener {
                            if (cont.isActive) cont.resume(null)
                        }
                    } ?: return null

                    val expectedSize = expectedW * expectedH
                    if (maskBuffer.remaining() < expectedSize) return null
                    val out = FloatArray(expectedSize)
                    val dup = maskBuffer.duplicate()
                    dup.rewind()
                    dup.get(out, 0, expectedSize)
                    out
                } finally {
                    runCatching { segmenter.close() }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                // Missing Play Services / native GMS / Robolectric JVM offline fallback -> deterministic L*a*b* takes over seamlessly.
                null
            }
        }

        private fun hasUsableMaskVariance(mask: FloatArray): Boolean {
            if (mask.isEmpty()) return false
            var above = 0
            var below = 0
            for (i in mask.indices step 4) {
                val v = mask[i]
                if (v >= 0.55f) above++ else if (v <= 0.25f) below++
            }
            val sampled = (mask.size / 4).coerceAtLeast(1)
            return above > (sampled * 0.02f) && below > (sampled * 0.08f)
        }
    }

    inner class OcclusionCompositionSubEngine {

        suspend fun synthesizeComposition(
            packet: ForegroundExtractionPacket,
            titleHint: String,
            startMs: Long,
            onStageProgress: ((DepthScanStage) -> Unit)?
        ): DepthDetectionResult {
            val cw = packet.cw
            val ch = packet.ch
            val onlinePred = packet.onlinePrediction
            val usedOnline = onlinePred != null
            val engineSource = when {
                usedOnline && packet.usedMlKit -> "Online Depth Engine V5 (Cloud Vision + ML Kit + L*a*b*)"
                usedOnline -> "Online Depth Engine V5 (Cloud Vision + L*a*b*)"
                packet.usedMlKit -> "Offline ML Kit + CIE L*a*b* Depth V5"
                else -> "Offline CIE L*a*b* Depth V5 Pure"
            }

            if (onlinePred != null && !onlinePred.isForegroundSubject && onlinePred.confidence >= 0.72f) {
                return flatResult(
                    titleHint = onlinePred.subjectLabel.ifBlank { titleHint },
                    processingTimeMs = SystemClock.elapsedRealtime() - startMs,
                    skyL = packet.topBorderStats.meanL,
                    skyA = packet.topBorderStats.meanA,
                    skyB = packet.topBorderStats.meanB,
                    engineSource = engineSource
                )
            }

            val saliencyContrast = computeForegroundBackgroundContrast(packet.saliency)
            if (!usedOnline && !packet.usedMlKit && (saliencyContrast < MIN_SEPARATION_CONTRAST || packet.maxGeo < MIN_BARRIER_LAB)) {
                return flatResult(
                    titleHint = titleHint,
                    processingTimeMs = SystemClock.elapsedRealtime() - startMs,
                    skyL = packet.topBorderStats.meanL,
                    skyA = packet.topBorderStats.meanA,
                    skyB = packet.topBorderStats.meanB,
                    engineSource = engineSource
                )
            }

            coroutineContext.ensureActive()

            val geometry = evaluateDepthSubjectGeometry(
                mask = packet.cleanedMask,
                saliency = packet.saliency,
                w = cw,
                h = ch,
                skyStats = packet.topBorderStats
            )

            if (!geometry.isEligible) {
                return DepthDetectionResult(
                    subjectLabel = "No isolated foreground",
                    subjectCategory = DepthSubjectCategory.FLAT_CANVAS,
                    isDepthEligible = false,
                    depthQualityScore = geometry.qualityScore,
                    cutoutBitmap = null,
                    recommendedClockTopFraction = onlinePred?.clockTopFraction ?: 0.14f,
                    recommendedAlign = onlinePred?.clockAlign ?: ClockHorizontalAlign.CENTER,
                    recommendedClockColorHex = LabColorSpace.pickNaturalHarmonicClockHex(
                        packet.topBorderStats.meanL,
                        packet.topBorderStats.meanA,
                        packet.topBorderStats.meanB
                    ),
                    subjectTopFraction = geometry.peakYFraction,
                    subjectPeakXFraction = geometry.peakXFraction,
                    foregroundCoverageFraction = geometry.coverageFraction,
                    processingTimeMs = SystemClock.elapsedRealtime() - startMs,
                    requiresRefinement = false,
                    engineSource = engineSource
                )
            }

            onStageProgress?.invoke(DepthScanStage.MATTE_SYNTHESIS)

            val coarseAlpha = buildSoftCoarseAlpha(packet.cleanedMask, packet.saliency, packet.tau, cw, ch)
            val cutoutBitmap = upsampleCutoutLabGuided(
                fullBitmap = packet.fullBmp,
                coarseAlpha = coarseAlpha,
                coarseL = packet.rawL,
                coarseA = packet.rawA,
                coarseB = packet.rawB,
                cw = cw,
                ch = ch
            )

            val edgeCrispness = estimateBoundaryEdgeQuality(packet.cleanedMask, packet.edgeMag, cw, ch)
            val finalCategory = onlinePred?.category?.takeIf { it != DepthSubjectCategory.FLAT_CANVAS } ?: geometry.category
            val finalClockTop = if (onlinePred != null && onlinePred.confidence >= 0.65f) {
                (0.55f * onlinePred.clockTopFraction + 0.45f * geometry.recommendedClockTopFraction).coerceIn(0.08f, 0.34f)
            } else {
                geometry.recommendedClockTopFraction
            }
            val finalAlign = onlinePred?.clockAlign ?: geometry.recommendedAlign

            val clockColorHex = sampleClockRegionContrastHex(
                smL = packet.smL,
                smA = packet.smA,
                smB = packet.smB,
                mask = packet.cleanedMask,
                cw = cw,
                ch = ch,
                clockTopFrac = finalClockTop,
                fallbackStats = packet.topBorderStats
            )

            val label = onlinePred?.subjectLabel?.takeIf { it.isNotBlank() }
                ?: inferSubjectLabel(finalCategory, titleHint)
            val finalQuality = if (usedOnline) {
                max(geometry.qualityScore, ((onlinePred?.confidence ?: 0.8f) * 95f).roundToInt().coerceIn(60, 99))
            } else {
                geometry.qualityScore
            }
            val needsRefinement = finalQuality < 56 || edgeCrispness < 0.44f
            val warningText = if (needsRefinement) {
                "Soft edge separation detected ($finalQuality%). Refine edges with the Keep / Erase brush."
            } else {
                null
            }

            return DepthDetectionResult(
                subjectLabel = label,
                subjectCategory = finalCategory,
                isDepthEligible = true,
                depthQualityScore = finalQuality,
                cutoutBitmap = cutoutBitmap,
                recommendedClockTopFraction = finalClockTop,
                recommendedAlign = finalAlign,
                recommendedClockColorHex = clockColorHex,
                subjectTopFraction = geometry.peakYFraction,
                subjectPeakXFraction = geometry.peakXFraction,
                foregroundCoverageFraction = geometry.coverageFraction,
                processingTimeMs = SystemClock.elapsedRealtime() - startMs,
                recommendedClockOverlapFraction = geometry.clockOcclusionFraction,
                recommendedWallpaperZoom = geometry.recommendedWallpaperZoom,
                recommendedWallpaperPanXFraction = geometry.recommendedWallpaperPanXFraction,
                recommendedWallpaperPanYFraction = geometry.recommendedWallpaperPanYFraction,
                recommendedClockScale = geometry.recommendedClockScale,
                layers = emptyList(),
                requiresRefinement = needsRefinement,
                confidenceWarning = warningText,
                engineSource = engineSource
            )
        }
    }

    private fun computeLabSobel(
        lArr: FloatArray,
        aArr: FloatArray,
        bArr: FloatArray,
        outEdge: FloatArray,
        w: Int,
        h: Int
    ) {
        for (y in 1 until h - 1) {
            val rowU = (y - 1) * w
            val rowC = y * w
            val rowD = (y + 1) * w
            for (x in 1 until w - 1) {
                val tl = rowU + x - 1
                val tc = rowU + x
                val tr = rowU + x + 1
                val ml = rowC + x - 1
                val mr = rowC + x + 1
                val bl = rowD + x - 1
                val bc = rowD + x
                val br = rowD + x + 1

                val gxL = (lArr[tr] + 2f * lArr[mr] + lArr[br]) - (lArr[tl] + 2f * lArr[ml] + lArr[bl])
                val gyL = (lArr[bl] + 2f * lArr[bc] + lArr[br]) - (lArr[tl] + 2f * lArr[tc] + lArr[tr])
                val gxA = (aArr[tr] + 2f * aArr[mr] + aArr[br]) - (aArr[tl] + 2f * aArr[ml] + aArr[bl])
                val gyA = (aArr[bl] + 2f * aArr[bc] + aArr[br]) - (aArr[tl] + 2f * aArr[tc] + aArr[tr])
                val gxB = (bArr[tr] + 2f * bArr[mr] + bArr[br]) - (bArr[tl] + 2f * bArr[ml] + bArr[bl])
                val gyB = (bArr[bl] + 2f * bArr[bc] + bArr[br]) - (bArr[tl] + 2f * bArr[tc] + bArr[tr])

                val mag = sqrt(gxL * gxL + gyL * gyL + gxA * gxA + gyA * gyA + gxB * gxB + gyB * gyB) * 0.125f
                outEdge[rowC + x] = mag
            }
        }
    }

    private fun buildTopSkyModel(
        lArr: FloatArray,
        aArr: FloatArray,
        bArr: FloatArray,
        edgeMag: FloatArray,
        skySeeds: BooleanArray,
        w: Int,
        h: Int
    ): SkyModel {
        val topBandRows = max(2, (h * 0.015f).roundToInt())
        var seedCount = 0
        for (y in 0 until topBandRows) {
            val row = y * w
            for (x in 0 until w) {
                val idx = row + x
                if (edgeMag[idx] < 6.5f) {
                    skySeeds[idx] = true
                    seedCount++
                }
            }
        }
        if (seedCount < w / 3) {
            for (x in 0 until w) {
                skySeeds[x] = true
            }
        }

        var sumL = 0f
        var sumA = 0f
        var sumB = 0f
        var cnt = 0
        val sampleMaxY = max(4, (h * 0.12f).roundToInt())
        for (y in 0 until sampleMaxY step 2) {
            val row = y * w
            for (x in 0 until w step 2) {
                val idx = row + x
                if (edgeMag[idx] < 5.0f) {
                    sumL += lArr[idx]
                    sumA += aArr[idx]
                    sumB += bArr[idx]
                    cnt++
                }
            }
        }
        if (cnt == 0) {
            for (x in 0 until w) {
                sumL += lArr[x]
                sumA += aArr[x]
                sumB += bArr[x]
                cnt++
            }
        }
        val meanL = sumL / cnt
        val meanA = sumA / cnt
        val meanB = sumB / cnt

        val pL = FloatArray(3)
        val pA = FloatArray(3)
        val pB = FloatArray(3)
        val regions = intArrayOf(0, w / 3, (2 * w) / 3, w)
        val upperH = max(4, (h * 0.16f).roundToInt())
        for (r in 0 until 3) {
            var rL = 0f
            var rA = 0f
            var rB = 0f
            var rC = 0
            for (y in 0 until upperH step 2) {
                val row = y * w
                for (x in regions[r] until regions[r + 1] step 2) {
                    val idx = row + x
                    if (edgeMag[idx] < 5.5f) {
                        rL += lArr[idx]
                        rA += aArr[idx]
                        rB += bArr[idx]
                        rC++
                    }
                }
            }
            if (rC > 0) {
                pL[r] = rL / rC
                pA[r] = rA / rC
                pB[r] = rB / rC
            } else {
                pL[r] = meanL
                pA[r] = meanA
                pB[r] = meanB
            }
        }

        val marginMaxY = (h * 0.45f).roundToInt().coerceIn(2, h - 1)
        var leftOpen = skySeeds[0]
        var rightOpen = skySeeds[w - 1]
        for (y in 1 until marginMaxY) {
            val leftIdx = y * w
            val prevLeft = leftIdx - w
            if (leftOpen) {
                val step = LabColorSpace.deltaE(
                    lArr[leftIdx], aArr[leftIdx], bArr[leftIdx],
                    lArr[prevLeft], aArr[prevLeft], bArr[prevLeft]
                )
                if (edgeMag[leftIdx] < 4.5f && step < 3.0f) {
                    skySeeds[leftIdx] = true
                } else {
                    leftOpen = false
                }
            }
            val rightIdx = leftIdx + w - 1
            val prevRight = prevLeft + w - 1
            if (rightOpen) {
                val step = LabColorSpace.deltaE(
                    lArr[rightIdx], aArr[rightIdx], bArr[rightIdx],
                    lArr[prevRight], aArr[prevRight], bArr[prevRight]
                )
                if (edgeMag[rightIdx] < 4.5f && step < 3.0f) {
                    skySeeds[rightIdx] = true
                } else {
                    rightOpen = false
                }
            }
            if (!leftOpen && !rightOpen) break
        }

        return SkyModel(pL, pA, pB, meanL, meanA, meanB)
    }

    private fun runGeodesicBarrierPasses(
        dist: FloatArray,
        lArr: FloatArray,
        aArr: FloatArray,
        bArr: FloatArray,
        edgeMag: FloatArray,
        w: Int,
        h: Int
    ) {
        repeat(2) {
            for (y in 0 until h) {
                val row = y * w
                for (x in 0 until w) {
                    val p = row + x
                    var best = dist[p]
                    if (x > 0) {
                        val c = stepCost(p - 1, p, 1f, dist, lArr, aArr, bArr, edgeMag)
                        if (c < best) best = c
                    }
                    if (y > 0) {
                        val q = p - w
                        val c = stepCost(q, p, 1f, dist, lArr, aArr, bArr, edgeMag)
                        if (c < best) best = c
                        if (x > 0) {
                            val cd = stepCost(q - 1, p, 1.35f, dist, lArr, aArr, bArr, edgeMag)
                            if (cd < best) best = cd
                        }
                        if (x < w - 1) {
                            val cd = stepCost(q + 1, p, 1.35f, dist, lArr, aArr, bArr, edgeMag)
                            if (cd < best) best = cd
                        }
                    }
                    dist[p] = best
                }
            }

            for (y in h - 1 downTo 0) {
                val row = y * w
                for (x in w - 1 downTo 0) {
                    val p = row + x
                    var best = dist[p]
                    if (x < w - 1) {
                        val c = stepCost(p + 1, p, 1f, dist, lArr, aArr, bArr, edgeMag)
                        if (c < best) best = c
                    }
                    if (y < h - 1) {
                        val q = p + w
                        val c = stepCost(q, p, 1f, dist, lArr, aArr, bArr, edgeMag)
                        if (c < best) best = c
                        if (x > 0) {
                            val cd = stepCost(q - 1, p, 1.35f, dist, lArr, aArr, bArr, edgeMag)
                            if (cd < best) best = cd
                        }
                        if (x < w - 1) {
                            val cd = stepCost(q + 1, p, 1.35f, dist, lArr, aArr, bArr, edgeMag)
                            if (cd < best) best = cd
                        }
                    }
                    dist[p] = best
                }
            }
        }
    }

    private fun stepCost(
        q: Int,
        p: Int,
        diagWeight: Float,
        dist: FloatArray,
        lArr: FloatArray,
        aArr: FloatArray,
        bArr: FloatArray,
        edgeMag: FloatArray
    ): Float {
        val dq = dist[q]
        if (dq == Float.POSITIVE_INFINITY) return Float.POSITIVE_INFINITY
        val dL = lArr[p] - lArr[q]
        val dA = aArr[p] - aArr[q]
        val dB = bArr[p] - bArr[q]
        val stepDeltaE = sqrt(dL * dL + dA * dA + dB * dB)
        val barrierColor = max(0f, stepDeltaE - SMOOTH_SKY_DEADZONE)
        val barrierEdge = max(0f, edgeMag[p] - 2.0f) * 0.48f
        return dq + (barrierColor + barrierEdge) * diagWeight
    }

    private fun computeSkyPrototypeDivergence(
        lArr: FloatArray,
        aArr: FloatArray,
        bArr: FloatArray,
        skyModel: SkyModel,
        outDiv: FloatArray
    ) {
        val pL = skyModel.protoL
        val pA = skyModel.protoA
        val pB = skyModel.protoB
        for (i in lArr.indices) {
            val l = lArr[i]
            val a = aArr[i]
            val b = bArr[i]
            val d0 = LabColorSpace.deltaE(l, a, b, pL[0], pA[0], pB[0])
            val d1 = LabColorSpace.deltaE(l, a, b, pL[1], pA[1], pB[1])
            val d2 = LabColorSpace.deltaE(l, a, b, pL[2], pA[2], pB[2])
            outDiv[i] = min(d0, min(d1, d2))
        }
    }

    private fun percentile(values: FloatArray, quantile: Float): Float {
        val hist = IntArray(256)
        var maxV = 0f
        for (v in values) {
            if (v.isFinite() && v > maxV) maxV = v
        }
        if (maxV <= 0f) return 0f
        val inv = 255f / maxV
        for (v in values) {
            val bin = if (v.isFinite()) (v * inv).toInt().coerceIn(0, 255) else 255
            hist[bin]++
        }
        val target = (values.size * quantile).toInt()
        var accumulated = 0
        for (i in 0 until 256) {
            accumulated += hist[i]
            if (accumulated >= target) {
                return (i.toFloat() / 255f) * maxV
            }
        }
        return maxV
    }

    private fun computeForegroundBackgroundContrast(saliency: FloatArray): Float {
        var topSum = 0f
        var botSum = 0f
        val third = saliency.size / 3
        for (i in 0 until third) topSum += saliency[i]
        for (i in (saliency.size - third) until saliency.size) botSum += saliency[i]
        val topMean = topSum / third.coerceAtLeast(1)
        val botMean = botSum / third.coerceAtLeast(1)
        return (botMean - topMean).coerceIn(0f, 1f)
    }

    private fun computeOtsuThreshold(saliency: FloatArray): Float {
        val hist = IntArray(100)
        for (s in saliency) {
            val bin = (s * 99f).toInt().coerceIn(0, 99)
            hist[bin]++
        }
        val total = saliency.size
        var sum = 0f
        for (i in 0 until 100) sum += i * hist[i]

        var sumB = 0f
        var wB = 0
        var maxVariance = 0f
        var bestThreshold = 50

        for (i in 0 until 100) {
            wB += hist[i]
            if (wB == 0) continue
            val wF = total - wB
            if (wF == 0) break
            sumB += i * hist[i]
            val mB = sumB / wB
            val mF = (sum - sumB) / wF
            val varBetween = wB.toFloat() * wF.toFloat() * (mB - mF) * (mB - mF)
            if (varBetween > maxVariance) {
                maxVariance = varBetween
                bestThreshold = i
            }
        }
        return bestThreshold.toFloat() / 100f
    }

    private fun filterComponentsAndCloseHoles(
        mask: BooleanArray,
        saliency: FloatArray,
        w: Int,
        h: Int
    ): ComponentAnalysisResult {
        val n = w * h
        val visited = BooleanArray(n)
        val queue = IntArray(n)
        val keep = BooleanArray(n)
        val minCandidateArea = max(10, (n * 0.003f).toInt())
        val minMainArea = max(12, (n * 0.010f).toInt())

        val candidates = ArrayList<CandidateComponent>(8)

        for (start in 0 until n) {
            if (!mask[start] || visited[start]) continue
            var head = 0
            var tail = 0
            queue[tail++] = start
            visited[start] = true

            var sumSal = 0f
            var minY = h
            var maxY = 0
            var minX = w
            var maxX = 0
            while (head < tail) {
                val p = queue[head++]
                sumSal += saliency[p]
                val py = p / w
                val px = p - py * w
                if (py < minY) minY = py
                if (py > maxY) maxY = py
                if (px < minX) minX = px
                if (px > maxX) maxX = px

                if (px > 0 && mask[p - 1] && !visited[p - 1]) {
                    visited[p - 1] = true; queue[tail++] = p - 1
                }
                if (px < w - 1 && mask[p + 1] && !visited[p + 1]) {
                    visited[p + 1] = true; queue[tail++] = p + 1
                }
                if (py > 0 && mask[p - w] && !visited[p - w]) {
                    visited[p - w] = true; queue[tail++] = p - w
                }
                if (py < h - 1 && mask[p + w] && !visited[p + w]) {
                    visited[p + w] = true; queue[tail++] = p + w
                }
            }

            val area = tail
            if (area < minCandidateArea) continue
            val meanSal = sumSal / area
            val touchesBottom = maxY >= h - max(4, (h * 0.03f).roundToInt())
            val isSignificant = area >= minMainArea ||
                (meanSal > 0.62f && minY > h * 0.04f && maxY < h * 0.90f)
            if (!isSignificant) continue

            val topEdgePenalty = if (minY <= max(2, (h * 0.025f).roundToInt()) && maxY < h * 0.40f) 0.25f else 1.0f
            val anchorBonus = if (touchesBottom) 1.65f else 1.0f
            val score = area.toFloat() * meanSal * anchorBonus * topEdgePenalty

            candidates.add(
                CandidateComponent(
                    pixels = queue.copyOf(area),
                    area = area,
                    meanSaliency = meanSal,
                    minY = minY,
                    maxY = maxY,
                    minX = minX,
                    maxX = maxX,
                    touchesBottom = touchesBottom,
                    dominanceScore = score
                )
            )
        }

        val dominant = candidates.maxByOrNull { it.dominanceScore }
        if (dominant != null) {
            for (p in dominant.pixels) keep[p] = true
            val proxY = (h * 0.12f).roundToInt()
            val proxX = (w * 0.18f).roundToInt()
            val minCompanionArea = (dominant.area * 0.12f).roundToInt().coerceAtLeast(minMainArea)
            for (comp in candidates) {
                if (comp !== dominant && comp.area >= minCompanionArea) {
                    val gapX = max(0, max(comp.minX - dominant.maxX, dominant.minX - comp.maxX))
                    val gapY = max(0, max(comp.minY - dominant.maxY, dominant.minY - comp.maxY))
                    if (gapX <= proxX && gapY <= proxY) {
                        for (p in comp.pixels) keep[p] = true
                    }
                }
            }
        }

        return ComponentAnalysisResult(keep)
    }

    /**
     * Fills 1-2px interior background pinholes inside solid foreground masks without dilating outer contours.
     */
    private fun closeSmallInteriorHoles(mask: BooleanArray, w: Int, h: Int): BooleanArray {
        val out = mask.copyOf()
        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val i = row + x
                if (!mask[i]) {
                    var fgNeighbors = 0
                    if (mask[i - 1]) fgNeighbors++
                    if (mask[i + 1]) fgNeighbors++
                    if (mask[i - w]) fgNeighbors++
                    if (mask[i + w]) fgNeighbors++
                    if (fgNeighbors >= 3) {
                        out[i] = true
                    }
                }
            }
        }
        return out
    }

    private fun evaluateDepthSubjectGeometry(
        mask: BooleanArray,
        saliency: FloatArray,
        w: Int,
        h: Int,
        @Suppress("UNUSED_PARAMETER") skyStats: SkyModel
    ): DepthGeometryEval {
        val n = w * h
        var fgCount = 0
        var minY = h
        var maxY = -1
        var minX = w
        var maxX = -1

        var topBandCount = 0
        var topBandMeanX = 0f

        val topClearEnd = (h * 0.08f).roundToInt().coerceAtLeast(1)
        var topClearFg = 0

        val clockBandStart = (h * 0.10f).roundToInt()
        val clockBandEnd = (h * 0.56f).roundToInt().coerceAtMost(h)
        var clockBandFg = 0

        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                if (!mask[row + x]) continue
                fgCount++
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < topClearEnd) topClearFg++
                if (y in clockBandStart until clockBandEnd) clockBandFg++
            }
        }

        val coverage = fgCount.toFloat() / n.toFloat()
        if (fgCount < (n * 0.012f) || coverage > 0.88f || minY >= h) {
            return DepthGeometryEval(
                isEligible = false,
                category = DepthSubjectCategory.FLAT_CANVAS,
                qualityScore = 15,
                peakYFraction = 0.5f,
                peakXFraction = 0.5f,
                coverageFraction = coverage,
                recommendedClockTopFraction = 0.14f,
                recommendedAlign = ClockHorizontalAlign.CENTER,
                clockOcclusionFraction = 0f
            )
        }

        val peakCrownEnd = min(h, minY + max(4, (h * 0.05f).roundToInt()))
        for (y in minY until peakCrownEnd) {
            val row = y * w
            for (x in 0 until w) {
                if (mask[row + x]) {
                    topBandMeanX += x
                    topBandCount++
                }
            }
        }

        val peakYFrac = minY.toFloat() / h.toFloat()
        val peakXFrac = if (topBandCount > 0) (topBandMeanX / topBandCount) / w.toFloat() else 0.5f
        val bottomYFrac = maxY.toFloat() / h.toFloat()
        val widthFrac = (maxX - minX + 1).toFloat() / w.toFloat()
        val heightFrac = (maxY - minY + 1).toFloat() / h.toFloat()
        val topClearOcclusion = topClearFg.toFloat() / (topClearEnd * w).toFloat()
        val clockBandCoverage = clockBandFg.toFloat() / max(1, (clockBandEnd - clockBandStart) * w).toFloat()

        val entersClockZone = peakYFrac in 0.03f..0.54f && clockBandCoverage >= 0.018f
        val hasOpenHeadroom = topClearOcclusion < 0.72f
        val isWellSeparated = entersClockZone && hasOpenHeadroom

        val isFloatingOrb = bottomYFrac < 0.84f &&
            peakYFrac > 0.05f &&
            widthFrac in 0.12f..0.72f &&
            abs(widthFrac - heightFrac) < 0.25f

        val category = when {
            !isWellSeparated -> DepthSubjectCategory.FLAT_CANVAS
            isFloatingOrb -> DepthSubjectCategory.CELESTIAL_ORB
            widthFrac > 0.78f && bottomYFrac > 0.90f -> DepthSubjectCategory.SUMMIT_SILHOUETTE
            widthFrac < 0.45f -> DepthSubjectCategory.BOTANICAL_ARCH
            else -> DepthSubjectCategory.PORTRAIT_FIGURE
        }

        val quality = if (!isWellSeparated) {
            (coverage * 40f).roundToInt().coerceIn(10, 45)
        } else {
            val anchorBonus = if (bottomYFrac >= 0.92f) 18 else 8
            val headroomScore = ((1f - topClearOcclusion) * 35f).roundToInt()
            val salScore = ((saliency.average().toFloat()) * 30f).roundToInt()
            (anchorBonus + headroomScore + salScore).coerceIn(48, 98)
        }

        val align = when {
            peakXFrac < 0.38f -> ClockHorizontalAlign.END
            peakXFrac > 0.62f -> ClockHorizontalAlign.START
            else -> ClockHorizontalAlign.CENTER
        }

        val clockTop = (peakYFrac - 0.08f).coerceIn(0.08f, 0.32f)
        val occlusion = (clockBandCoverage * 0.42f).coerceIn(0.05f, 0.45f)

        return DepthGeometryEval(
            isEligible = isWellSeparated,
            category = category,
            qualityScore = quality,
            peakYFraction = peakYFrac,
            peakXFraction = peakXFrac,
            coverageFraction = coverage,
            recommendedClockTopFraction = clockTop,
            recommendedAlign = align,
            clockOcclusionFraction = occlusion,
            recommendedWallpaperZoom = 1.0f,
            recommendedWallpaperPanXFraction = 0f,
            recommendedWallpaperPanYFraction = 0f,
            recommendedClockScale = 1.0f
        )
    }

    private fun buildSoftCoarseAlpha(
        mask: BooleanArray,
        saliency: FloatArray,
        tau: Float,
        w: Int,
        h: Int
    ): FloatArray {
        val out = FloatArray(w * h)
        for (i in out.indices) {
            out[i] = if (mask[i]) {
                val sal = saliency[i]
                if (sal >= tau + 0.12f) 1f
                else ((sal - (tau - 0.04f)) / 0.16f).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
        return out
    }

    private fun upsampleCutoutLabGuided(
        fullBitmap: Bitmap,
        coarseAlpha: FloatArray,
        coarseL: FloatArray,
        coarseA: FloatArray,
        coarseB: FloatArray,
        cw: Int,
        ch: Int
    ): Bitmap {
        val fw = fullBitmap.width
        val fh = fullBitmap.height
        val fullCount = fw * fh
        val srcPixels = IntArray(fullCount)
        fullBitmap.getPixels(srcPixels, 0, fw, 0, 0, fw, fh)

        val outPixels = IntArray(fullCount)
        val scaleX = cw.toFloat() / fw.toFloat()
        val scaleY = ch.toFloat() / fh.toFloat()
        val invSigmaLabSq = 1f / (11f * 11f)
        val labBuf = FloatArray(3)

        val mapX0 = IntArray(fw)
        val mapX1 = IntArray(fw)
        val mapFx = FloatArray(fw)
        for (x in 0 until fw) {
            val gx = ((x + 0.5f) * scaleX - 0.5f).coerceIn(0f, (cw - 1).toFloat())
            val x0 = floor(gx).toInt()
            mapX0[x] = x0
            mapX1[x] = min(x0 + 1, cw - 1)
            mapFx[x] = gx - x0
        }

        for (y in 0 until fh) {
            val gy = ((y + 0.5f) * scaleY - 0.5f).coerceIn(0f, (ch - 1).toFloat())
            val y0 = floor(gy).toInt()
            val y1 = min(y0 + 1, ch - 1)
            val fy = gy - y0
            val oneMinusFy = 1f - fy

            val cRow0 = y0 * cw
            val cRow1 = y1 * cw
            val fRow = y * fw

            for (x in 0 until fw) {
                val x0 = mapX0[x]
                val x1 = mapX1[x]
                val i00 = cRow0 + x0
                val i10 = cRow0 + x1
                val i01 = cRow1 + x0
                val i11 = cRow1 + x1

                val a00 = coarseAlpha[i00]
                val a10 = coarseAlpha[i10]
                val a01 = coarseAlpha[i01]
                val a11 = coarseAlpha[i11]

                val fIdx = fRow + x
                if (a00 >= 0.99f && a10 >= 0.99f && a01 >= 0.99f && a11 >= 0.99f) {
                    outPixels[fIdx] = srcPixels[fIdx]
                    continue
                }
                if (a00 <= 0.01f && a10 <= 0.01f && a01 <= 0.01f && a11 <= 0.01f) {
                    outPixels[fIdx] = 0
                    continue
                }

                val srcArgb = srcPixels[fIdx]
                LabColorSpace.argbToLab(srcArgb, labBuf)
                val fL = labBuf[0]
                val fA = labBuf[1]
                val fB = labBuf[2]

                val fx = mapFx[x]
                val oneMinusFx = 1f - fx

                fun labWeight(idx: Int, spatialWeight: Float): Float {
                    val dl = fL - coarseL[idx]
                    val da = fA - coarseA[idx]
                    val db = fB - coarseB[idx]
                    val de2 = dl * dl + 0.72f * (da * da + db * db)
                    return spatialWeight / (1f + de2 * invSigmaLabSq)
                }

                val w00 = labWeight(i00, oneMinusFx * oneMinusFy)
                val w10 = labWeight(i10, fx * oneMinusFy)
                val w01 = labWeight(i01, oneMinusFx * fy)
                val w11 = labWeight(i11, fx * fy)

                val wSum = w00 + w10 + w01 + w11
                val rawAlpha = if (wSum > 1e-6f) {
                    (w00 * a00 + w10 * a10 + w01 * a01 + w11 * a11) / wSum
                } else {
                    0f
                }

                val t = ((rawAlpha - 0.16f) / 0.68f).coerceIn(0f, 1f)
                val smoothAlpha = t * t * (3f - 2f * t)
                val alphaByte = (smoothAlpha * 255f).roundToInt().coerceIn(0, 255)
                outPixels[fIdx] = if (alphaByte == 0) {
                    0
                } else {
                    (alphaByte shl 24) or (srcArgb and 0x00FFFFFF)
                }
            }
        }

        val cutout = ZeroCrashEngine.safeCreateBitmap(fw, fh)
            ?: throw OutOfMemoryError("Unable to allocate cutout bitmap (${fw}x${fh})")
        cutout.setPixels(outPixels, 0, fw, 0, 0, fw, fh)
        return cutout
    }

    private fun estimateBoundaryEdgeQuality(
        mask: BooleanArray,
        edgeMag: FloatArray,
        w: Int,
        h: Int
    ): Float {
        var edgeSum = 0f
        var edgeCnt = 0
        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val i = row + x
                if (mask[i]) {
                    val isBoundary = !mask[i - 1] || !mask[i + 1] || !mask[i - w] || !mask[i + w]
                    if (isBoundary) {
                        edgeSum += edgeMag[i]
                        edgeCnt++
                    }
                }
            }
        }
        return if (edgeCnt > 0) (edgeSum / edgeCnt / 12f).coerceIn(0.2f, 1f) else 0.5f
    }

    private fun sampleClockRegionContrastHex(
        smL: FloatArray,
        smA: FloatArray,
        smB: FloatArray,
        mask: BooleanArray,
        cw: Int,
        ch: Int,
        clockTopFrac: Float,
        fallbackStats: SkyModel
    ): String {
        val yStart = (clockTopFrac * ch).toInt().coerceIn(0, ch - 1)
        val yEnd = ((clockTopFrac + 0.18f) * ch).toInt().coerceIn(0, ch)
        var sumL = 0f
        var sumA = 0f
        var sumB = 0f
        var cnt = 0
        for (y in yStart until yEnd) {
            val row = y * cw
            for (x in (cw * 0.2f).toInt() until (cw * 0.8f).toInt()) {
                val i = row + x
                if (!mask[i]) {
                    sumL += smL[i]
                    sumA += smA[i]
                    sumB += smB[i]
                    cnt++
                }
            }
        }
        return if (cnt > 0) {
            LabColorSpace.pickNaturalHarmonicClockHex(sumL / cnt, sumA / cnt, sumB / cnt)
        } else {
            LabColorSpace.pickNaturalHarmonicClockHex(fallbackStats.meanL, fallbackStats.meanA, fallbackStats.meanB)
        }
    }

    private fun inferSubjectLabel(category: DepthSubjectCategory, titleHint: String): String {
        return when (category) {
            DepthSubjectCategory.CELESTIAL_ORB -> "Celestial Orbit"
            DepthSubjectCategory.SUMMIT_SILHOUETTE -> "Summit Ridge"
            DepthSubjectCategory.BOTANICAL_ARCH -> "Botanical Silhouette"
            DepthSubjectCategory.PORTRAIT_FIGURE -> if (titleHint.isNotBlank()) titleHint else "Foreground Subject"
            DepthSubjectCategory.FLAT_CANVAS -> "Flat Canvas"
        }
    }

    private fun flatResult(
        titleHint: String,
        processingTimeMs: Long,
        skyL: Float,
        skyA: Float,
        skyB: Float,
        engineSource: String = "CIE L*a*b* Depth V5 Pure"
    ): DepthDetectionResult = DepthDetectionResult(
        subjectLabel = titleHint.ifBlank { "Canvas" },
        subjectCategory = DepthSubjectCategory.FLAT_CANVAS,
        isDepthEligible = false,
        depthQualityScore = 20,
        cutoutBitmap = null,
        recommendedClockTopFraction = 0.14f,
        recommendedAlign = ClockHorizontalAlign.CENTER,
        recommendedClockColorHex = LabColorSpace.pickNaturalHarmonicClockHex(skyL, skyA, skyB),
        subjectTopFraction = 0.5f,
        subjectPeakXFraction = 0.5f,
        foregroundCoverageFraction = 0f,
        processingTimeMs = processingTimeMs,
        requiresRefinement = false,
        engineSource = engineSource
    )

    private fun ensureSoftwareArgb8888(bitmap: Bitmap): Bitmap? {
        val safe = ZeroCrashEngine.guardBitmap(bitmap, requireSoftware = true) ?: return null
        return if (safe.config == Bitmap.Config.ARGB_8888) {
            safe
        } else {
            runCatching { safe.copy(Bitmap.Config.ARGB_8888, false) }.getOrNull()
        }
    }

    private fun prepareBoundedWorkBitmap(bitmap: Bitmap, maxLongEdge: Int): Bitmap? {
        val soft = ensureSoftwareArgb8888(bitmap) ?: return null
        val longest = max(soft.width, soft.height)
        if (longest <= maxLongEdge) return soft
        val ratio = maxLongEdge.toFloat() / longest.toFloat()
        val w = (soft.width * ratio).roundToInt().coerceAtLeast(32)
        val h = (soft.height * ratio).roundToInt().coerceAtLeast(32)
        return runCatching { soft.scale(w, h) }.getOrNull() ?: soft
    }

    companion object {
        private const val ANALYSIS_WIDTH = 420
        private const val MAX_CUTOUT_LONG_EDGE = 1152
        private const val SMOOTH_SKY_DEADZONE = 1.85f
        private const val MIN_BARRIER_LAB = 6.5f
        private const val MIN_SEPARATION_CONTRAST = 0.20f
    }
}
