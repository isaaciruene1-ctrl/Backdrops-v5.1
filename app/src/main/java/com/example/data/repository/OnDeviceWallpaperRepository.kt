package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.data.model.ColorFamily
import com.example.data.model.DepthSubjectHint
import com.example.data.model.DiscoveryCollection
import com.example.data.model.ResolutionTier
import com.example.data.model.UserPreferencesDoc
import com.example.data.model.Wallpaper
import com.example.data.model.WallpaperCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Play-policy compliant On-Device Wallpaper Repository.
 *
 * Uses strictly scoped Photo Picker (`PickMultipleVisualMedia`) and Storage Access Framework
 * (`OpenDocumentTree`) URIs persisted in [UserPreferencesDoc] without ever requesting broad
 * storage permissions (`READ_EXTERNAL_STORAGE` or `READ_MEDIA_IMAGES`).
 *
 * Hiding the collection in Backdrop never deletes user files from device storage.
 */
object OnDeviceWallpaperRepository {

    private val _items = MutableStateFlow<List<Wallpaper>>(emptyList())
    val items: StateFlow<List<Wallpaper>> = _items.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun takePersistableReadPermission(context: Context, uri: Uri, isTree: Boolean) {
        val resolver = context.contentResolver
        runCatching {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            resolver.takePersistableUriPermission(uri, flags)
        }
        if (isTree) {
            runCatching {
                DocumentsContract.getTreeDocumentId(uri)
            }
        }
    }

    suspend fun scanOnDeviceWallpapers(
        context: Context,
        prefs: UserPreferencesDoc
    ): List<Wallpaper> {
        if (!prefs.onDeviceCollectionEnabled) {
            _items.value = emptyList()
            return emptyList()
        }
        _isScanning.value = true
        return try {
            val resolved = withContext(Dispatchers.IO) {
                val appContext = context.applicationContext
                val resolver = appContext.contentResolver
                val collected = mutableListOf<Wallpaper>()
                val seenUris = LinkedHashSet<String>()

                // 1. User-picked individual photos via Photo Picker
                prefs.onDevicePhotoUris.forEachIndexed { idx, rawUri ->
                    val uri = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return@forEachIndexed
                    if (!seenUris.add(uri.toString())) return@forEachIndexed
                    buildWallpaperFromUri(resolver, uri, idx, "Photo Library")?.let { collected += it }
                }

                // 2. User-authorized SAF folders via OpenDocumentTree
                prefs.onDeviceFolderUris.forEach { treeRaw ->
                    val treeUri = runCatching { Uri.parse(treeRaw) }.getOrNull() ?: return@forEach
                    val folderItems = scanDocumentTree(resolver, treeUri, collected.size)
                    folderItems.forEach { wp ->
                        if (seenUris.add(wp.fullResUrl)) {
                            collected += wp
                        }
                    }
                }
                collected
            }
            _items.value = resolved
            resolved
        } finally {
            _isScanning.value = false
        }
    }

    private fun scanDocumentTree(
        resolver: ContentResolver,
        treeUri: Uri,
        startOffset: Int
    ): List<Wallpaper> {
        val results = mutableListOf<Wallpaper>()
        val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            ?: return emptyList()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        runCatching {
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                var index = startOffset
                while (cursor.moveToNext() && results.size < MAX_FOLDER_SCAN_ITEMS) {
                    val docId = if (idIdx >= 0) cursor.getString(idIdx) else null
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) else null
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    if (docId.isNullOrBlank() || mime == null || !mime.startsWith("image/", ignoreCase = true)) {
                        continue
                    }
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    results += createOnDeviceWallpaper(
                        uriString = docUri.toString(),
                        displayName = cleanTitle(name ?: "Device Image"),
                        orderIndex = index++,
                        sourceLabel = "Device Folder"
                    )
                }
            }
        }
        return results
    }

    private fun buildWallpaperFromUri(
        resolver: ContentResolver,
        uri: Uri,
        index: Int,
        sourceLabel: String
    ): Wallpaper? {
        val readable = runCatching {
            resolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        }.getOrDefault(false)
        if (!readable) return null

        var displayName: String? = null
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) displayName = cursor.getString(idx)
                }
            }
        }
        return createOnDeviceWallpaper(
            uriString = uri.toString(),
            displayName = cleanTitle(displayName ?: "Device Canvas ${index + 1}"),
            orderIndex = index,
            sourceLabel = sourceLabel
        )
    }

    internal fun createOnDeviceWallpaper(
        uriString: String,
        displayName: String,
        orderIndex: Int,
        sourceLabel: String
    ): Wallpaper {
        val idHash = shortStableHash(uriString)
        return Wallpaper(
            id = "ondevice_$idHash",
            title = displayName,
            description = "Scoped local device image ($sourceLabel) ready for Depth Studio & Lock Screen Mode.",
            category = WallpaperCategory.DEPTH_STUDIO,
            collections = listOf(DiscoveryCollection.PHOTOGRAPHY, DiscoveryCollection.DEPTH_STUDIO),
            thumbnailUrl = uriString,
            fullResUrl = uriString,
            fallbackResId = 0,
            width = 1440,
            height = 2560,
            resolutionTier = ResolutionTier.QHD_1440,
            aspectRatio = "9:16",
            dominantColors = listOf(0xFF1E2434, 0xFF5674A6, 0xFFB8CBE8),
            colorFamily = ColorFamily.COOL,
            tags = listOf("On Device", sourceLabel, "Local"),
            creatorName = sourceLabel,
            creatorSource = "On Device",
            isDownloadable = false,
            supportsDepthEffect = true,
            depthSubjectHint = DepthSubjectHint.CENTER_PORTRAIT,
            popularityScore = 88,
            addedOrder = 10_000 + orderIndex
        )
    }

    fun formatUriLabel(rawUri: String): String {
        val uri = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return rawUri
        val docId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        val rawTail = (docId ?: uri.lastPathSegment ?: rawUri)
            .substringAfterLast(':')
            .substringAfterLast('/')
        return rawTail.ifBlank { "Selected Folder" }
    }

    private fun cleanTitle(raw: String): String {
        val stripped = raw.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim()
        return stripped.ifBlank { "Device Image" }.take(38)
    }

    private fun shortStableHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.take(6).joinToString("") { "%02x".format(it) }
    }

    private const val MAX_FOLDER_SCAN_ITEMS = 120
}
