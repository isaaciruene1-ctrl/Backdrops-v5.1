package com.example.data.media

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.model.Wallpaper
import com.example.engine.performance.ZeroLagPerformanceEngine
import com.example.widget.BackdropWidgetContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class WallpaperTarget {
    HOME, LOCK, BOTH
}

sealed interface DownloadProgress {
    data object Starting : DownloadProgress
    data class InProgress(val fraction: Float, val bytesRead: Long, val totalBytes: Long) : DownloadProgress
    data class Completed(val uri: Uri, val fileName: String, val isDuplicateHandled: Boolean) : DownloadProgress
    data class Failed(val message: String) : DownloadProgress
}

/**
 * Image Loading Engine V6 — Sub-Engine A: Bounded Non-Blocking HTTP Client.
 * - Uses a standard mobile Chrome User-Agent & image Accept headers so image CDNs never challenge custom bot strings.
 * - Caps concurrent per-host sockets (`maxRequests = 18`, `maxRequestsPerHost = 4`) to prevent LTE socket starvation.
 * - Relies strictly on Coil's DiskCache (no double OkHttp cache) and rejects non-image MIME responses immediately
 *   so multi-CDN URL failover engages without blocking dispatcher threads.
 */
object PooledHttpRetrySubEngine {
    const val FORCE_RELOAD_HEADER = "X-Backdrop-Force-Reload"
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    private const val ACCEPT_IMAGE = "image/jpeg,image/png,image/webp;q=0.9,*/*;q=0.5"

    fun buildSharedClient(): OkHttpClient {
        val dispatcher = Dispatcher().apply {
            maxRequests = 18
            maxRequestsPerHost = 4
        }
        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(ConnectionPool(8, 2, TimeUnit.MINUTES))
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(24, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val incoming = chain.request()
                val forceReload = incoming.header(FORCE_RELOAD_HEADER) == "1"
                val builder = incoming.newBuilder()
                    .removeHeader(FORCE_RELOAD_HEADER)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", ACCEPT_IMAGE)
                if (incoming.url.host.contains("unsplash.com") && incoming.url.queryParameter("auto") == "format") {
                    builder.url(
                        incoming.url.newBuilder()
                            .removeAllQueryParameters("auto")
                            .setQueryParameter("fm", "jpg")
                            .build()
                    )
                }
                if (forceReload) {
                    builder.cacheControl(CacheControl.FORCE_NETWORK)
                }
                val res = chain.proceed(builder.build())
                if (!res.isSuccessful && res.code != 206) {
                    val code = res.code
                    res.close()
                    throw IOException("HTTP $code for ${incoming.url}")
                }
                val contentType = res.header("Content-Type")?.lowercase().orEmpty()
                if (contentType.isNotEmpty() &&
                    (
                        (!contentType.startsWith("image/") && !contentType.contains("octet-stream")) ||
                            contentType.contains("avif") ||
                            contentType.contains("heif") ||
                            contentType.contains("heic")
                        )
                ) {
                    res.close()
                    throw IOException("Unsupported image Content-Type '$contentType' for ${incoming.url}")
                }
                res
            }
            .build()
    }
}

object BackdropImageLoader {
    private const val DISK_CACHE_DIR_NAME = "backdrop_image_cache_v2"

    @Volatile
    private var instance: ImageLoader? = null

    @Volatile
    private var sharedHttpClient: OkHttpClient? = null

    fun createOkHttpClient(context: Context): OkHttpClient {
        return sharedHttpClient ?: synchronized(this) {
            sharedHttpClient ?: PooledHttpRetrySubEngine.buildSharedClient().also { sharedHttpClient = it }
        }
    }

    fun get(context: Context): ImageLoader {
        val appContext = context.applicationContext
        ZeroLagPerformanceEngine.init(appContext)
        return instance ?: synchronized(this) {
            instance ?: run {
                val legacyCacheDir = File(appContext.cacheDir, "backdrop_image_cache")
                if (legacyCacheDir.exists()) {
                    runCatching { legacyCacheDir.deleteRecursively() }
                }
                val tier = ZeroLagPerformanceEngine.classifyTier(appContext)
                ImageLoader.Builder(appContext)
                    .okHttpClient { createOkHttpClient(appContext) }
                    .memoryCache {
                        MemoryCache.Builder(appContext)
                            .maxSizePercent(ZeroLagPerformanceEngine.memoryCachePercent(tier))
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(File(appContext.cacheDir, DISK_CACHE_DIR_NAME))
                            .maxSizeBytes(ZeroLagPerformanceEngine.diskCacheLimitBytes(tier))
                            .build()
                    }
                    .respectCacheHeaders(false)
                    .crossfade(220)
                    .build()
                    .also { instance = it }
            }
        }
    }

    fun getCacheSizeBytes(context: Context): Long {
        val dir = File(context.applicationContext.cacheDir, DISK_CACHE_DIR_NAME)
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun evictWallpaperCache(context: Context, wallpaperId: String) {
        val loader = get(context)
        val thumbKey = "${wallpaperId}_thumb"
        val fullKey = "${wallpaperId}_full"
        loader.memoryCache?.remove(MemoryCache.Key(thumbKey))
        loader.memoryCache?.remove(MemoryCache.Key(fullKey))
        runCatching {
            loader.diskCache?.remove(thumbKey)
            loader.diskCache?.remove(fullKey)
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        val loader = get(context)
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
    }
}

class WallpaperMediaService(private val context: Context) {

    private val client: OkHttpClient
        get() = BackdropImageLoader.createOkHttpClient(context)

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun downloadWallpaper(wallpaper: Wallpaper): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Starting)
        val sanitizedTitle = wallpaper.title
            .replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
            .trim()
            .replace(" ", "_")
            .lowercase()
            .ifEmpty { wallpaper.id }
        val baseName = "Backdrops_${sanitizedTitle}_${wallpaper.id}"

        val bytes = if (wallpaper.isBundledAsset && wallpaper.fallbackResId != 0) {
            decodeDrawableToJpeg(wallpaper.fallbackResId)
        } else {
            val diskCache = BackdropImageLoader.get(context).diskCache
            val cachedBytes = runCatching {
                listOf("${wallpaper.id}_full", wallpaper.fullResUrl).firstNotNullOfOrNull { key ->
                    diskCache?.openSnapshot(key)?.use { snap ->
                        snap.data.toFile().takeIf { it.exists() && it.length() > 1024L }?.readBytes()
                    }
                }
            }.getOrNull()

            if (cachedBytes != null && cachedBytes.isNotEmpty()) {
                emit(DownloadProgress.InProgress(1f, cachedBytes.size.toLong(), cachedBytes.size.toLong()))
                cachedBytes
            } else {
                var downloaded = ByteArray(0)
                val urls = (
                    CanonicalSourceLockSubEngine.resolveLockedCandidates(
                        context = context,
                        wallpaper = wallpaper,
                        useFullRes = true,
                        forDownload = true
                    ) + wallpaper.candidateUrls(useFullRes = true)
                    ).distinct()
                for (url in urls) {
                    val attempt = try {
                        fetchBytesResumableWithProgress(wallpaper.id, url) { progress, read, total ->
                            emit(DownloadProgress.InProgress(progress, read, total))
                        }
                    } catch (_: Exception) {
                        ByteArray(0)
                    }
                    if (attempt.isNotEmpty()) {
                        downloaded = attempt
                        break
                    }
                }
                downloaded
            }
        }

        if (bytes.isEmpty()) {
            emit(DownloadProgress.Failed("Unable to retrieve wallpaper image bytes. Please check your connection and retry."))
            return@flow
        }

        val (savedUri, finalName, renamed) = saveToPictures(bytes, baseName)
        if (savedUri != null) {
            emit(DownloadProgress.Completed(savedUri, finalName, renamed))
        } else {
            emit(DownloadProgress.Failed("Could not write image to Pictures/Backdrops."))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun saveBitmapToPictures(bitmap: Bitmap, titlePrefix: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                val (uri, _, _) = saveToPictures(stream.toByteArray(), titlePrefix)
                if (uri != null) Result.success(uri)
                else Result.failure(IllegalStateException("MediaStore failed to store image."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun applyWallpaper(
        wallpaper: Wallpaper,
        target: WallpaperTarget,
        allowHomeParallax: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadFullBitmap(wallpaper)
                ?: return@withContext Result.failure(IllegalStateException("Unable to decode wallpaper bitmap."))
            applyBitmapAsWallpaper(bitmap, target, allowHomeParallax)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Wallpaper Synchronization Engine V5 — Sub-Engine A & B:
     * Centered ARGB_8888 target application + atomic lock canvas file mirroring + widget contract broadcast.
     */
    private object WallpaperSyncSubEngine {
        fun applyBitmapTarget(
            appContext: Context,
            bitmap: Bitmap,
            target: WallpaperTarget,
            allowHomeParallax: Boolean
        ): Result<Unit> {
            val manager = WallpaperManager.getInstance(appContext)
            if (!manager.isWallpaperSupported) {
                return Result.failure(IllegalStateException("Setting wallpapers is not supported on this device."))
            }
            if (!manager.isSetWallpaperAllowed) {
                return Result.failure(IllegalStateException("Device policy restricts changing the wallpaper."))
            }

            val screenAspect = com.example.ui.screens.resolveDevicePortraitAspect(appContext)
            val parallaxEnabled = allowHomeParallax && target != WallpaperTarget.LOCK
            val cropRect = com.example.ui.screens.computeCenteredViewportCropRect(
                sourceWidth = bitmap.width,
                sourceHeight = bitmap.height,
                zoomScale = 1f,
                panOffsetX = 0f,
                panOffsetY = 0f,
                viewportWidthPx = 0f,
                viewportHeightPx = 0f,
                targetScreenAspect = screenAspect,
                allowParallaxWidth = parallaxEnabled
            )
            val alignedBitmap = if (
                cropRect.left == 0 &&
                cropRect.top == 0 &&
                cropRect.width == bitmap.width &&
                cropRect.height == bitmap.height
            ) {
                bitmap
            } else {
                Bitmap.createBitmap(bitmap, cropRect.left, cropRect.top, cropRect.width, cropRect.height)
            }

            val safeBitmap = if (alignedBitmap.config == Bitmap.Config.ARGB_8888) {
                alignedBitmap
            } else {
                alignedBitmap.copy(Bitmap.Config.ARGB_8888, false) ?: alignedBitmap
            }

            val cropHint: android.graphics.Rect? = if (parallaxEnabled) {
                null
            } else {
                android.graphics.Rect(0, 0, safeBitmap.width, safeBitmap.height)
            }

            val flags = when (target) {
                WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
            val jpegBytes = ByteArrayOutputStream().use { out ->
                safeBitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
                out.toByteArray()
            }
            val streamId = ByteArrayInputStream(jpegBytes).use { input ->
                manager.setStream(input, cropHint, true, flags)
            }
            if (streamId == 0) {
                val bmpId = manager.setBitmap(safeBitmap, cropHint, true, flags)
                if (bmpId == 0) {
                    return Result.failure(
                        IllegalStateException("System WallpaperManager rejected the wallpaper update.")
                    )
                }
            }
            return Result.success(Unit)
        }

        fun persistLockCanvasBitmap(appContext: Context, bitmap: Bitmap): String? = runCatching {
            val tmpFile = File(appContext.filesDir, "active_lock_canvas.jpg.tmp")
            val outFile = File(appContext.filesDir, "active_lock_canvas.jpg")
            FileOutputStream(tmpFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            if (outFile.exists()) outFile.delete()
            tmpFile.renameTo(outFile)
            outFile.absolutePath
        }.getOrNull()
    }

    suspend fun applyBitmapAsWallpaper(
        bitmap: Bitmap,
        target: WallpaperTarget,
        allowHomeParallax: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            WallpaperSyncSubEngine.applyBitmapTarget(
                appContext = context.applicationContext,
                bitmap = bitmap,
                target = target,
                allowHomeParallax = allowHomeParallax
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveActiveLockWallpaperBitmap(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        WallpaperSyncSubEngine.persistLockCanvasBitmap(context.applicationContext, bitmap)
    }

    suspend fun clearActiveLockCustomBitmap() = withContext(Dispatchers.IO) {
        runCatching {
            val outFile = File(context.applicationContext.filesDir, "active_lock_canvas.jpg")
            if (outFile.exists()) {
                outFile.delete()
            }
        }
        Unit
    }

    fun syncWidgetWithWallpaper(wallpaperId: String) {
        if (wallpaperId.isNotBlank()) {
            BackdropWidgetContract.setCurrentWallpaperId(context.applicationContext, wallpaperId)
            BackdropWidgetContract.refreshAllWidgets(context.applicationContext)
        }
    }

    suspend fun shareWallpaper(wallpaper: Wallpaper) {
        val bitmap = withContext(Dispatchers.IO) { loadFullBitmap(wallpaper) } ?: return
        shareBitmap(bitmap, "${wallpaper.title} — Backdrops")
    }

    suspend fun shareBitmap(bitmap: Bitmap, shareTitle: String) {
        val uri = withContext(Dispatchers.IO) {
            val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(sharedDir, "backdrop_share_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "$shareTitle • YOUR SCREEN. YOUR CANVAS.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Wallpaper").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun calculateInSampleSize(rawWidth: Int, rawHeight: Int, maxLongEdge: Int = 1440): Int {
        var inSampleSize = 1
        val longest = maxOf(rawWidth, rawHeight).coerceAtLeast(1)
        while ((longest / inSampleSize) > maxLongEdge * 2) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun decodeBoundedResource(resId: Int, maxLongEdge: Int = 1440): Bitmap? {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inScaled = false
        }
        BitmapFactory.decodeResource(context.resources, resId, bounds)
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxLongEdge)
        }
        return runCatching { BitmapFactory.decodeResource(context.resources, resId, opts) }.getOrNull()
    }

    private fun decodeBoundedFile(path: String, maxLongEdge: Int = 1440): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxLongEdge)
        }
        return runCatching { BitmapFactory.decodeFile(path, opts) }.getOrNull()
    }

    private fun decodeBoundedBytes(bytes: ByteArray, maxLongEdge: Int = 1440): Bitmap? {
        if (bytes.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxLongEdge)
        }
        return runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) }.getOrNull()
    }

    private fun decodeBoundedUri(uri: Uri, maxLongEdge: Int = 1440): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxLongEdge)
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
    }

    /**
     * Image Loading Engine V5 — Sub-Engine B: Offline-First Multi-Tier Bitmap Resolver.
     * Resolves bundled assets, local document URIs, downloaded offline packs, full/thumb disk cache,
     * and finally pooled network fetch — never substituting unrelated static drawables.
     */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    suspend fun loadFullBitmap(wallpaper: Wallpaper): Bitmap? = withContext(Dispatchers.IO) {
        if (wallpaper.isBundledAsset && wallpaper.fallbackResId != 0) {
            return@withContext decodeBoundedResource(wallpaper.fallbackResId)
        }
        if (wallpaper.fullResUrl.startsWith("content://") || wallpaper.fullResUrl.startsWith("file://")) {
            val localBmp = runCatching {
                decodeBoundedUri(Uri.parse(wallpaper.fullResUrl))
            }.getOrNull()
            if (localBmp != null) return@withContext localBmp
        }
        OfflinePackDownloadManager.getDownloadedFile(context, wallpaper.id)?.let { offlineFile ->
            val offlineBmp = decodeBoundedFile(offlineFile.absolutePath)
            if (offlineBmp != null) return@withContext offlineBmp
        }
        // Check Coil disk cache under explicit full-res key first, keeping same-wallpaper thumb as offline fallback
        var cachedThumbFallback: Bitmap? = null
        try {
            val diskCache = BackdropImageLoader.get(context).diskCache
            for (key in listOf("${wallpaper.id}_full", wallpaper.fullResUrl, "${wallpaper.id}_thumb", wallpaper.thumbnailUrl)) {
                if (key.isBlank()) continue
                diskCache?.openSnapshot(key)?.use { snapshot ->
                    val cached = decodeBoundedFile(snapshot.data.toFile().absolutePath)
                    if (cached != null) {
                        if (key != "${wallpaper.id}_thumb" && key != wallpaper.thumbnailUrl) {
                            return@withContext cached
                        } else if (cachedThumbFallback == null) {
                            cachedThumbFallback = cached
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        val candidateUrls = CanonicalSourceLockSubEngine.resolveLockedCandidates(
            context = context,
            wallpaper = wallpaper,
            useFullRes = true,
            forDownload = false
        )
        for (url in candidateUrls) {
            val bmp = try {
                val request = Request.Builder()
                    .url(url)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null) decodeBoundedBytes(bytes) else null
                    } else null
                }
            } catch (_: Exception) {
                null
            }
            if (bmp != null) return@withContext bmp
        }
        cachedThumbFallback
    }

    /**
     * Download Manager V2: supports HTTP 206 Partial Content resume via temporary `.part` files
     * so interrupted large 4K downloads continue cleanly without re-fetching downloaded bytes.
     */
    private suspend fun fetchBytesResumableWithProgress(
        wallpaperId: String,
        url: String,
        onProgress: suspend (Float, Long, Long) -> Unit
    ): ByteArray {
        val dlDir = File(context.cacheDir, "dl_cache").apply { mkdirs() }
        val safeKey = wallpaperId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val partFile = File(dlDir, "$safeKey.part")
        var existingBytes = if (partFile.exists()) partFile.length() else 0L

        val reqBuilder = Request.Builder().url(url)
        if (existingBytes > 0L) {
            reqBuilder.header("Range", "bytes=$existingBytes-")
        }

        client.newCall(reqBuilder.build()).execute().use { response ->
            if (response.code == 416 && existingBytes > 0L) {
                partFile.delete()
                return fetchBytesResumableWithProgress(wallpaperId, url, onProgress)
            }
            if (!response.isSuccessful && response.code != 206) {
                error("HTTP ${response.code}")
            }
            val body = response.body ?: error("Empty body")
            val isPartialAppend = response.code == 206 && existingBytes > 0L
            if (!isPartialAppend) {
                existingBytes = 0L
            }
            val remaining = body.contentLength()
            val total = if (remaining > 0L) existingBytes + remaining else -1L
            val buffer = ByteArray(16384)
            var readTotal = existingBytes

            FileOutputStream(partFile, isPartialAppend).use { fileOut ->
                body.byteStream().use { input ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        fileOut.write(buffer, 0, read)
                        readTotal += read
                        val fraction = if (total > 0L) (readTotal.toFloat() / total).coerceIn(0f, 1f) else 0.5f
                        onProgress(fraction, readTotal, total)
                    }
                }
            }
            val resultBytes = partFile.readBytes()
            partFile.delete()
            return resultBytes
        }
    }

    private fun decodeDrawableToJpeg(resId: Int): ByteArray {
        val bmp = BitmapFactory.decodeResource(context.resources, resId) ?: return ByteArray(0)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 94, out)
        return out.toByteArray()
    }

    private fun saveToPictures(bytes: ByteArray, baseName: String): Triple<Uri?, String, Boolean> {
        val resolver = context.contentResolver
        var fileName = "$baseName.jpg"
        var renamed = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val existing = checkExistingMediaStore(fileName)
            if (existing) {
                fileName = "${baseName}_${System.currentTimeMillis() % 10000}.jpg"
                renamed = true
            }
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Backdrops")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return Triple(null, fileName, renamed)
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return Triple(uri, fileName, renamed)
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Backdrops").apply {
                mkdirs()
            }
            var file = File(dir, fileName)
            if (file.exists()) {
                fileName = "${baseName}_${System.currentTimeMillis() % 10000}.jpg"
                file = File(dir, fileName)
                renamed = true
            }
            FileOutputStream(file).use { it.write(bytes) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            return Triple(uri, fileName, renamed)
        }
    }

    private fun checkExistingMediaStore(displayName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        return context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(displayName),
            null
        )?.use { cursor -> cursor.count > 0 } ?: false
    }
}
