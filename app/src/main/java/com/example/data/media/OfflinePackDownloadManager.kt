package com.example.data.media

import android.content.Context
import com.example.data.catalog.WallpaperCatalog
import com.example.data.model.OfflinePackSizeOption
import com.example.data.model.Wallpaper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class OfflinePackProgress(
    val packOption: OfflinePackSizeOption = OfflinePackSizeOption.STUDIO,
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val bytesWritten: Long = 0L,
    val currentTitle: String = "",
    val isDownloading: Boolean = false,
    val isPaused: Boolean = false,
    val statusLabel: String = "NOT_DOWNLOADED"
) {
    val fraction: Float
        get() = if (totalCount <= 0) 0f else (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)

    val isComplete: Boolean
        get() = totalCount > 0 && completedCount >= totalCount && !isDownloading
}

/**
 * Manages optional curated offline wallpaper pack downloads in app-private storage (`filesDir/offline_packs`).
 * Prevents duplicates, reports transparent byte/item progress, and supports Pause, Resume, Cancel & Delete.
 */
object OfflinePackDownloadManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null

    private val _progress = MutableStateFlow(OfflinePackProgress())
    val progress: StateFlow<OfflinePackProgress> = _progress.asStateFlow()

    private val _downloadedIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedIds: StateFlow<Set<String>> = _downloadedIds.asStateFlow()

    fun offlineDir(context: Context): File =
        File(context.applicationContext.filesDir, "offline_packs").apply { if (!exists()) mkdirs() }

    fun getDownloadedFile(context: Context, wallpaperId: String): File? {
        val f = File(offlineDir(context), "$wallpaperId.jpg")
        return if (f.exists() && f.length() > 128L) f else null
    }

    fun scanDiskDownloadedIds(context: Context): List<String> {
        val dir = offlineDir(context)
        val files = dir.listFiles() ?: return emptyList()
        return files
            .filter { it.isFile && it.name.endsWith(".jpg") && it.length() > 128L }
            .sortedByDescending { it.lastModified() }
            .map { it.name.removeSuffix(".jpg") }
    }

    fun resolveOfflineWallpapers(
        downloadedIds: Collection<String>,
        context: Context? = null
    ): List<Wallpaper> {
        val diskIds = context?.let { scanDiskDownloadedIds(it) }.orEmpty()
        val combinedIds = LinkedHashSet<String>().apply {
            addAll(downloadedIds)
            addAll(diskIds)
        }
        return combinedIds.mapNotNull { WallpaperCatalog.getById(it) }
    }

    fun selectCandidateWallpapers(
        option: OfflinePackSizeOption,
        alreadyDownloadedIds: List<String>
    ): List<Wallpaper> {
        val all = WallpaperCatalog.allWallpapers
        val byPopularity = all.sortedByDescending { it.popularityScore }
        val chosen = byPopularity.take(option.count)
        return chosen.sortedBy { if (it.id in alreadyDownloadedIds) 0 else 1 }
    }

    fun syncPersistedState(
        context: Context,
        targetCount: Int,
        downloadedIds: List<String>,
        status: String
    ) {
        if (_progress.value.isDownloading) return
        val option = OfflinePackSizeOption.fromCount(targetCount)
        val diskIds = scanDiskDownloadedIds(context)
        val merged = LinkedHashSet<String>().apply {
            downloadedIds.filter { getDownloadedFile(context, it) != null }.forEach { add(it) }
            addAll(diskIds)
        }
        _downloadedIds.value = merged.toSet()
        val bytes = offlineDir(context).listFiles()?.sumOf { it.length() } ?: 0L
        val effectiveStatus = when {
            merged.isEmpty() -> if (status == "DOWNLOADING") "NOT_DOWNLOADED" else status
            merged.size >= option.count -> "COMPLETED"
            status == "COMPLETED" || status == "PARTIAL" || status == "PAUSED" -> status
            else -> "PARTIAL"
        }
        _progress.value = OfflinePackProgress(
            packOption = option,
            totalCount = if (merged.isNotEmpty()) maxOf(option.count, merged.size) else 0,
            completedCount = merged.size,
            bytesWritten = bytes,
            statusLabel = effectiveStatus
        )
    }

    fun startOrResumeDownload(
        context: Context,
        option: OfflinePackSizeOption,
        existingDownloadedIds: List<String>,
        onStatePersisted: (targetCount: Int, downloadedIds: List<String>, status: String) -> Unit
    ) {
        val appContext = context.applicationContext
        if (_progress.value.isDownloading && _progress.value.isPaused) {
            _progress.value = _progress.value.copy(isPaused = false, statusLabel = "DOWNLOADING")
            return
        }
        downloadJob?.cancel()
        downloadJob = scope.launch {
            val dir = offlineDir(appContext)
            val candidates = selectCandidateWallpapers(option, existingDownloadedIds)
            val completedIds = LinkedHashSet<String>()
            existingDownloadedIds.forEach { id ->
                if (getDownloadedFile(appContext, id) != null) completedIds.add(id)
            }
            scanDiskDownloadedIds(appContext).forEach { completedIds.add(it) }
            _downloadedIds.value = completedIds.toSet()

            var totalBytes = dir.listFiles()?.sumOf { it.length() } ?: 0L
            var failed = 0
            _progress.value = OfflinePackProgress(
                packOption = option,
                totalCount = candidates.size,
                completedCount = completedIds.size.coerceAtMost(candidates.size),
                failedCount = 0,
                bytesWritten = totalBytes,
                currentTitle = "Preparing offline pack...",
                isDownloading = true,
                isPaused = false,
                statusLabel = "DOWNLOADING"
            )
            onStatePersisted(option.count, completedIds.toList(), "DOWNLOADING")

            val client = BackdropImageLoader.createOkHttpClient(appContext)
            try {
                for (wp in candidates) {
                    while (_progress.value.isPaused) {
                        delay(200L)
                    }
                    val targetFile = File(dir, "${wp.id}.jpg")
                    if (targetFile.exists() && targetFile.length() > 128L) {
                        completedIds.add(wp.id)
                        _downloadedIds.value = completedIds.toSet()
                        _progress.value = _progress.value.copy(
                            completedCount = completedIds.size.coerceAtMost(candidates.size),
                            currentTitle = wp.title
                        )
                        continue
                    }

                    _progress.value = _progress.value.copy(currentTitle = wp.title)
                    val ok = wp.candidateUrls(useFullRes = false).any { url ->
                        runCatching {
                            val req = Request.Builder().url(url).build()
                            client.newCall(req).execute().use { resp ->
                                if (!resp.isSuccessful) return@use false
                                val body = resp.body ?: return@use false
                                val tempFile = File(dir, "${wp.id}.tmp")
                                FileOutputStream(tempFile).use { out ->
                                    body.byteStream().copyTo(out)
                                }
                                if (tempFile.length() > 128L) {
                                    if (targetFile.exists()) targetFile.delete()
                                    tempFile.renameTo(targetFile)
                                } else {
                                    tempFile.delete()
                                    false
                                }
                            }
                        }.getOrDefault(false)
                    }

                    if (ok && targetFile.exists()) {
                        completedIds.add(wp.id)
                        totalBytes += targetFile.length()
                        _downloadedIds.value = completedIds.toSet()
                    } else {
                        failed++
                    }

                    val completedCount = completedIds.size.coerceAtMost(candidates.size)
                    _progress.value = _progress.value.copy(
                        completedCount = completedCount,
                        failedCount = failed,
                        bytesWritten = totalBytes
                    )
                    if (completedIds.size == 1 || completedIds.size % 5 == 0 || completedCount == candidates.size) {
                        onStatePersisted(option.count, completedIds.toList(), "DOWNLOADING")
                    }
                }

                val finalStatus = if (completedIds.size >= candidates.size) "COMPLETED" else "PARTIAL"
                _downloadedIds.value = completedIds.toSet()
                _progress.value = _progress.value.copy(
                    isDownloading = false,
                    isPaused = false,
                    currentTitle = if (finalStatus == "COMPLETED") "Offline Pack Ready" else "Pack Ready (${completedIds.size}/${candidates.size})",
                    statusLabel = finalStatus
                )
                onStatePersisted(option.count, completedIds.toList(), finalStatus)
            } catch (_: CancellationException) {
                val diskSynced = scanDiskDownloadedIds(appContext)
                completedIds.addAll(diskSynced)
                _downloadedIds.value = completedIds.toSet()
                _progress.value = _progress.value.copy(
                    isDownloading = false,
                    isPaused = false,
                    completedCount = completedIds.size.coerceAtMost(candidates.size),
                    currentTitle = "Paused at ${completedIds.size}/${candidates.size}",
                    statusLabel = "PAUSED"
                )
                onStatePersisted(option.count, completedIds.toList(), "PAUSED")
            }
        }
    }

    fun pauseDownload() {
        if (_progress.value.isDownloading) {
            _progress.value = _progress.value.copy(isPaused = true, statusLabel = "PAUSED")
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _progress.value = _progress.value.copy(isDownloading = false, isPaused = false, statusLabel = "CANCELLED")
    }

    fun clearOfflinePack(
        context: Context,
        onStatePersisted: (targetCount: Int, downloadedIds: List<String>, status: String) -> Unit
    ) {
        downloadJob?.cancel()
        val dir = offlineDir(context)
        dir.listFiles()?.forEach { it.delete() }
        _downloadedIds.value = emptySet()
        _progress.value = OfflinePackProgress(statusLabel = "NOT_DOWNLOADED")
        onStatePersisted(OfflinePackSizeOption.STUDIO.count, emptyList(), "NOT_DOWNLOADED")
    }
}
