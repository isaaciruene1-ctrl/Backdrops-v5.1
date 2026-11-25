package com.example.engine.depth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.graphics.scale
import com.example.engine.crash.ZeroCrashEngine
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class NormalizedPoint(
    val x: Float,
    val y: Float
)

data class NormalizedBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class OnlineVisionDepthPrediction(
    val isForegroundSubject: Boolean,
    val subjectLabel: String,
    val category: DepthSubjectCategory,
    val crownX: Float,
    val crownY: Float,
    val clockTopFraction: Float,
    val clockAlign: ClockHorizontalAlign,
    val foregroundPolygons: List<List<NormalizedPoint>>,
    val foregroundBoxes: List<NormalizedBox>,
    val confidence: Float
) {
    val hasGeometry: Boolean
        get() = foregroundPolygons.any { it.size >= 3 } || foregroundBoxes.isNotEmpty()
}

interface OnlineDepthVisionBackend {
    suspend fun analyzeForeground(
        bitmap: Bitmap,
        titleHint: String,
        preferPortraitPrior: Boolean
    ): OnlineVisionDepthPrediction?
}

/**
 * Online-First Multimodal Depth Vision Solver powered by Firebase AI Logic (`gemini-3.8-flash`).
 *
 * When internet connectivity is validated, extracts pinpoint subject polygons, crown crest,
 * category classification, and clock framing hints via structured JSON schema, then rasterizes
 * the polygon silhouette for sub-pixel CIE L*a*b* edge snapping inside [DepthDetectionEngine].
 */
object OnlineVisionDepthSolver {

    private const val TAG = "OnlineVisionDepth"
    private const val VISION_TIMEOUT_MS = 45_000L
    private const val MAX_VISION_LONG_EDGE = 640

    /**
     * Returns true strictly when the device has an active, internet-capable & validated network.
     * Returns false when offline / no data (including null Context in pure JVM tests).
     */
    fun isOnlineNetworkAvailable(context: Context?): Boolean {
        if (context == null) return false
        return runCatching {
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }.getOrDefault(false)
    }

    /**
     * Rasterizes normalized cloud vision polygons (or bounding box fallback) into a soft `[0..1]`
     * coarse prior buffer (`cw * ch`) ready for CIE L*a*b* contour snapping.
     */
    fun rasterizePriorMask(
        prediction: OnlineVisionDepthPrediction,
        cw: Int,
        ch: Int
    ): FloatArray {
        val w = cw.coerceAtLeast(1)
        val h = ch.coerceAtLeast(1)
        val n = w * h
        if (!prediction.isForegroundSubject || !prediction.hasGeometry) {
            return FloatArray(n)
        }

        val maskBmp = ZeroCrashEngine.safeCreateBitmap(w, h)
            ?: return FloatArray(n)
        val canvas = Canvas(maskBmp)
        canvas.drawColor(android.graphics.Color.TRANSPARENT)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }

        val validPolys = prediction.foregroundPolygons.filter { it.size >= 3 }
        if (validPolys.isNotEmpty()) {
            for (poly in validPolys) {
                val path = Path()
                val first = poly.first()
                path.moveTo(first.x.coerceIn(0f, 1f) * w, first.y.coerceIn(0f, 1f) * h)
                for (idx in 1 until poly.size) {
                    val pt = poly[idx]
                    path.lineTo(pt.x.coerceIn(0f, 1f) * w, pt.y.coerceIn(0f, 1f) * h)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
        } else {
            paint.alpha = (0.82f * 255f).roundToInt()
            for (box in prediction.foregroundBoxes) {
                val l = (min(box.left, box.right).coerceIn(0f, 1f) * w)
                val t = (min(box.top, box.bottom).coerceIn(0f, 1f) * h)
                val r = (max(box.left, box.right).coerceIn(0f, 1f) * w)
                val b = (max(box.top, box.bottom).coerceIn(0f, 1f) * h)
                canvas.drawRect(l, t, r, b, paint)
            }
        }

        val pixels = IntArray(n)
        maskBmp.getPixels(pixels, 0, w, 0, 0, w, h)
        if (!maskBmp.isRecycled) maskBmp.recycle()

        val raw = FloatArray(n) { i -> ((pixels[i] ushr 24) and 0xFF) / 255f }
        // Gentle 2px smoothing softens discrete polygon vertices into an organic snap band for CIE L*a*b*
        return separableBoxBlur(raw, w, h, radius = 2)
    }

    private fun separableBoxBlur(src: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        val r = radius.coerceIn(1, 8)
        val n = w * h
        val tmp = FloatArray(n)
        val out = FloatArray(n)
        val denom = (2 * r + 1).toFloat()

        for (y in 0 until h) {
            val row = y * w
            var acc = 0f
            for (dx in -r..r) acc += src[row + dx.coerceIn(0, w - 1)]
            for (x in 0 until w) {
                tmp[row + x] = acc / denom
                acc += src[row + (x + r + 1).coerceIn(0, w - 1)] - src[row + (x - r).coerceIn(0, w - 1)]
            }
        }
        for (x in 0 until w) {
            var acc = 0f
            for (dy in -r..r) acc += tmp[dy.coerceIn(0, h - 1) * w + x]
            for (y in 0 until h) {
                out[y * w + x] = acc / denom
                acc += tmp[(y + r + 1).coerceIn(0, h - 1) * w + x] - tmp[(y - r).coerceIn(0, h - 1) * w + x]
            }
        }
        return out
    }

    internal fun parseStructuredPredictionJson(
        jsonString: String,
        titleHint: String
    ): OnlineVisionDepthPrediction? {
        return runCatching {
            val root = JSONObject(jsonString)
            val isFg = root.optBoolean("isForegroundSubject", false)
            val label = root.optString("subjectLabel", "").trim().ifBlank {
                titleHint.ifBlank { "Foreground Subject" }
            }
            val category = runCatching {
                DepthSubjectCategory.valueOf(root.optString("category", "PORTRAIT_FIGURE").uppercase())
            }.getOrDefault(if (isFg) DepthSubjectCategory.PORTRAIT_FIGURE else DepthSubjectCategory.FLAT_CANVAS)

            val crownX = root.optDouble("crownX", 0.5).toFloat().coerceIn(0.05f, 0.95f)
            val crownY = root.optDouble("crownY", 0.25).toFloat().coerceIn(0.03f, 0.90f)
            val clockTop = root.optDouble("clockTopFraction", 0.14).toFloat().coerceIn(0.07f, 0.36f)
            val align = runCatching {
                ClockHorizontalAlign.valueOf(root.optString("clockAlign", "CENTER").uppercase())
            }.getOrDefault(ClockHorizontalAlign.CENTER)
            val confidence = root.optDouble("confidence", 0.85).toFloat().coerceIn(0f, 1f)

            val polygons = parsePolygons(root.optJSONArray("polygons"))
            val boxes = parseBoxes(root.optJSONArray("boxes"))

            OnlineVisionDepthPrediction(
                isForegroundSubject = isFg,
                subjectLabel = label,
                category = category,
                crownX = crownX,
                crownY = crownY,
                clockTopFraction = clockTop,
                clockAlign = align,
                foregroundPolygons = polygons,
                foregroundBoxes = boxes,
                confidence = confidence
            )
        }.onFailure { err ->
            Log.e(TAG, "Failed parsing Online Vision depth JSON", err)
        }.getOrNull()
    }

    private fun parsePolygons(arr: JSONArray?): List<List<NormalizedPoint>> {
        if (arr == null) return emptyList()
        val result = ArrayList<List<NormalizedPoint>>(arr.length())
        for (i in 0 until arr.length()) {
            val polyArr = arr.optJSONArray(i) ?: continue
            val pts = ArrayList<NormalizedPoint>(polyArr.length())
            for (j in 0 until polyArr.length()) {
                val obj = polyArr.optJSONObject(j) ?: continue
                val x = obj.optDouble("x", Double.NaN).toFloat()
                val y = obj.optDouble("y", Double.NaN).toFloat()
                if (x.isFinite() && y.isFinite()) {
                    pts.add(NormalizedPoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f)))
                }
            }
            if (pts.size >= 3) {
                result.add(pts)
            }
        }
        return result
    }

    private fun parseBoxes(arr: JSONArray?): List<NormalizedBox> {
        if (arr == null) return emptyList()
        val result = ArrayList<NormalizedBox>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val l = obj.optDouble("left", Double.NaN).toFloat()
            val t = obj.optDouble("top", Double.NaN).toFloat()
            val r = obj.optDouble("right", Double.NaN).toFloat()
            val b = obj.optDouble("bottom", Double.NaN).toFloat()
            if (l.isFinite() && t.isFinite() && r.isFinite() && b.isFinite()) {
                result.add(
                    NormalizedBox(
                        left = min(l, r).coerceIn(0f, 1f),
                        top = min(t, b).coerceIn(0f, 1f),
                        right = max(l, r).coerceIn(0f, 1f),
                        bottom = max(t, b).coerceIn(0f, 1f)
                    )
                )
            }
        }
        return result
    }

    object FirebaseAiOnlineDepthBackend : OnlineDepthVisionBackend {

        private val pointSchema = Schema.obj(
            properties = mapOf(
                "x" to Schema.double(description = "Normalized X coordinate 0.0..1.0"),
                "y" to Schema.double(description = "Normalized Y coordinate 0.0..1.0")
            )
        )

        private val boxSchema = Schema.obj(
            properties = mapOf(
                "left" to Schema.double(description = "Normalized left X 0.0..1.0"),
                "top" to Schema.double(description = "Normalized top Y 0.0..1.0"),
                "right" to Schema.double(description = "Normalized right X 0.0..1.0"),
                "bottom" to Schema.double(description = "Normalized bottom Y 0.0..1.0")
            )
        )

        private val depthSchema = Schema.obj(
            properties = mapOf(
                "isForegroundSubject" to Schema.boolean(
                    description = "True if image has a distinct foreground subject suitable for lock-screen depth occlusion"
                ),
                "subjectLabel" to Schema.string(description = "Concise 1-3 word subject label"),
                "category" to Schema.enumeration(
                    values = listOf(
                        "CELESTIAL_ORB",
                        "SUMMIT_SILHOUETTE",
                        "PORTRAIT_FIGURE",
                        "BOTANICAL_ARCH",
                        "FLAT_CANVAS"
                    ),
                    description = "Subject geometry classification"
                ),
                "crownX" to Schema.double(description = "Normalized X of highest foreground crest 0.0..1.0"),
                "crownY" to Schema.double(description = "Normalized Y of highest foreground crest 0.0..1.0"),
                "clockTopFraction" to Schema.double(description = "Optimal clock top position 0.08..0.34"),
                "clockAlign" to Schema.enumeration(
                    values = listOf("START", "CENTER", "END"),
                    description = "Horizontal clock alignment to keep numerals balanced against subject"
                ),
                "confidence" to Schema.double(description = "Segmentation confidence 0.0..1.0"),
                "polygons" to Schema.array(
                    items = Schema.array(
                        items = pointSchema,
                        description = "Ordered contour vertices tracing the foreground subject edge accurately"
                    ),
                    description = "1 to 3 dense polygon contours (12-24 vertices each) hugging the foreground subject edges"
                ),
                "boxes" to Schema.array(
                    items = boxSchema,
                    description = "Optional tight bounding boxes for foreground subjects"
                )
            ),
            optionalProperties = listOf("boxes")
        )

        private val model by lazy {
            Firebase.ai.generativeModel(
                modelName = "gemini-3.8-flash",
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = depthSchema
                    temperature = 0.15f
                }
            )
        }

        override suspend fun analyzeForeground(
            bitmap: Bitmap,
            titleHint: String,
            preferPortraitPrior: Boolean
        ): OnlineVisionDepthPrediction? {
            if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null
            val scaledForVision = prepareVisionBitmap(bitmap) ?: return null
            val prompt = buildString {
                append("Analyze this lock-screen wallpaper image (hint: '$titleHint'")
                if (preferPortraitPrior) append(", user photo portrait priority")
                append(") for pinpoint foreground depth-effect clock occlusion. ")
                append("Identify the dominant foreground subject(s) separating from background sky or backdrop. ")
                append("Trace accurate ordered boundary polygon vertices in normalized [0..1] coordinates tightly hugging ")
                append("the outer silhouette (head, shoulders, mountain ridge, planet rim, leaves, monument). ")
                append("If this is purely flat texture or uniform abstract gradients with no separable foreground, set isForegroundSubject=false and category=FLAT_CANVAS.")
            }

            return try {
                val response = withTimeout(VISION_TIMEOUT_MS) {
                    model.generateContent(
                        content {
                            image(scaledForVision)
                            text(prompt)
                        }
                    )
                }
                val text = response.text
                if (text.isNullOrBlank()) {
                    null
                } else {
                    parseStructuredPredictionJson(text, titleHint)
                }
            } catch (tce: TimeoutCancellationException) {
                Log.w(TAG, "Online Depth Engine V5 timed out; falling back to offline engine", tce)
                null
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.w(TAG, "Online Depth Engine V5 unavailable (${e.localizedMessage}); falling back to offline engine")
                null
            } finally {
                if (scaledForVision !== bitmap && !scaledForVision.isRecycled) {
                    scaledForVision.recycle()
                }
            }
        }

        private fun prepareVisionBitmap(source: Bitmap): Bitmap? {
            val safe = ZeroCrashEngine.guardBitmap(source, requireSoftware = true) ?: return null
            val longest = max(safe.width, safe.height)
            if (longest <= MAX_VISION_LONG_EDGE) return safe
            val ratio = MAX_VISION_LONG_EDGE.toFloat() / longest.toFloat()
            val w = (safe.width * ratio).roundToInt().coerceAtLeast(32)
            val h = (safe.height * ratio).roundToInt().coerceAtLeast(32)
            val scaled = runCatching { safe.scale(w, h) }.getOrNull()
            if (scaled != null && scaled !== safe && safe !== source && !safe.isRecycled) {
                safe.recycle()
            }
            return scaled ?: safe
        }
    }
}
