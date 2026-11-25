package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.catalog.WallpaperCatalog
import com.example.data.media.OfflinePackProgress
import com.example.data.model.LockScreenSetDoc
import com.example.data.model.OfflinePackSizeOption
import com.example.data.model.UiStyleMode
import com.example.data.model.UserCollectionDoc
import com.example.data.model.Wallpaper
import com.example.ui.components.ClockDesignConfig
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.StudioSegmentedBar
import com.example.ui.components.TactileSurface
import com.example.ui.components.WallpaperAsyncImage
import com.example.ui.components.WallpaperGridCard
import com.example.ui.components.glassSpecularBorder
import com.example.ui.theme.AppMotion
import com.example.ui.theme.LocalReducedMotionFlag
import com.example.ui.theme.LocalUiStyleMode
import com.example.ui.theme.rememberStudioHaptics
import com.example.ui.viewmodel.SyncState

private enum class CollectionsTabSection(val label: String) {
    BOARDS("Boards"),
    OFFLINE("Offline"),
    SAVED_SETS("Saved Sets")
}

@Composable
fun FavoritesScreen(
    favoritesState: SyncState<List<Wallpaper>>,
    recentlyViewed: List<Wallpaper>,
    favoriteIds: Set<String>,
    columns: Int,
    onOpenWallpaper: (Wallpaper) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Your personal curated studio gallery",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (recentlyViewed.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Recently Viewed",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentlyViewed, key = { it.id }) { wp ->
                            WallpaperGridCard(
                                wallpaper = wp,
                                isFavorite = wp.id in favoriteIds,
                                onClick = { onOpenWallpaper(wp) },
                                onFavoriteToggle = { onToggleFavorite(wp.id) },
                                aspectRatio = 0.68f,
                                modifier = Modifier.width(136.dp)
                            )
                        }
                    }
                }
            }
        }

        when (favoritesState) {
            SyncState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is SyncState.Error -> {
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        EmptyStatePanel(
                            title = "Could not load favorites",
                            subtitle = favoritesState.message
                        )
                    }
                }
            }
            is SyncState.Ready -> {
                val list = favoritesState.data
                if (list.isEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            EmptyStatePanel(
                                title = "No favorites saved yet",
                                subtitle = "Tap the heart icon on any artwork in Backdrops to build your personal studio gallery.",
                                actionLabel = "Explore Wallpapers",
                                onAction = onExploreClick
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "Saved Canvases (${list.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                    val rows = list.chunked(columns)
                    items(rows) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { wp ->
                                WallpaperGridCard(
                                    wallpaper = wp,
                                    isFavorite = wp.id in favoriteIds,
                                    onClick = { onOpenWallpaper(wp) },
                                    onFavoriteToggle = { onToggleFavorite(wp.id) },
                                    aspectRatio = 0.65f,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(columns - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollectionsScreen(
    collectionsState: SyncState<List<UserCollectionDoc>>,
    lockScreenSetsState: SyncState<List<LockScreenSetDoc>> = SyncState.Ready(emptyList()),
    columns: Int = 1,
    onCreateCollection: (String, String) -> Unit,
    onRenameCollection: (UserCollectionDoc, String, String) -> Unit,
    onDeleteCollection: (UserCollectionDoc) -> Unit,
    onOpenCollectionDetail: (UserCollectionDoc) -> Unit,
    onOpenLockScreenSet: (LockScreenSetDoc) -> Unit = {},
    onToggleLockScreenSetFavorite: (String) -> Unit = {},
    onDeleteLockScreenSet: (LockScreenSetDoc) -> Unit = {},
    onDeleteLockScreenSets: ((Set<String>) -> Unit)? = null,
    offlineWallpapers: List<Wallpaper> = emptyList(),
    favoriteIds: Set<String> = emptySet(),
    onOpenWallpaper: (Wallpaper) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onOpenOfflineSettings: (() -> Unit)? = null,
    offlinePackProgress: OfflinePackProgress = OfflinePackProgress(),
    offlinePackTargetCount: Int = 50,
    onStartOfflinePack: (OfflinePackSizeOption) -> Unit = {},
    onPauseOfflinePack: () -> Unit = {},
    onCancelOfflinePack: () -> Unit = {},
    onClearOfflinePack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingCollection by remember { mutableStateOf<UserCollectionDoc?>(null) }
    var activeSection by rememberSaveable { mutableStateOf(CollectionsTabSection.BOARDS) }
    var showInlineManagePack by rememberSaveable { mutableStateOf(false) }
    var selectedOfflineOption by remember(offlinePackTargetCount) {
        mutableStateOf(OfflinePackSizeOption.fromCount(offlinePackTargetCount))
    }
    var previewDisplaySetCard by remember { mutableStateOf<GalleryPreviewSpec?>(null) }

    // Saved Sets multi-select & delete confirmation state
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedSetIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDeleteSingleSet by remember { mutableStateOf<LockScreenSetDoc?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    val uiStyle = LocalUiStyleMode.current
    val isGlassUi = uiStyle == UiStyleMode.FROSTED_GLASS_OS
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (isGlassUi) Color.Transparent else MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (activeSection == CollectionsTabSection.BOARDS) {
                val fabMod = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = if (columns > 1) 8.dp else 84.dp)
                    .testTag("create_collection_fab")
                if (isGlassUi) {
                    Surface(
                        onClick = { showCreateDialog = true },
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                        border = glassSpecularBorder(),
                        shadowElevation = 12.dp,
                        modifier = fabMod.size(58.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Create collection",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else {
                    FloatingActionButton(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = fabMod
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Create collection")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = when (activeSection) {
                            CollectionsTabSection.BOARDS -> "Collections"
                            CollectionsTabSection.OFFLINE -> "Offline Pack"
                            CollectionsTabSection.SAVED_SETS -> "Lock-Screen Sets"
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (activeSection) {
                            CollectionsTabSection.BOARDS -> "Curate personal mood boards & seasonal setups"
                            CollectionsTabSection.OFFLINE -> "${offlineWallpapers.size} setup-downloaded wallpapers available offline"
                            CollectionsTabSection.SAVED_SETS -> "Saved Depth Studio compositions ready to re-open or apply"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StudioSegmentedBar(
                    options = CollectionsTabSection.entries,
                    selected = activeSection,
                    labelProvider = { it.label },
                    onSelect = {
                        activeSection = it
                        isMultiSelectMode = false
                        selectedSetIds = emptySet()
                    },
                    tagPrefix = "collections_section"
                )

                if (activeSection == CollectionsTabSection.SAVED_SETS) {
                    val savedSets = (lockScreenSetsState as? SyncState.Ready)?.data.orEmpty()
                    if (savedSets.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("saved_sets_action_bar")
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isMultiSelectMode = !isMultiSelectMode
                                    if (!isMultiSelectMode) selectedSetIds = emptySet()
                                },
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("saved_sets_select_mode_btn")
                            ) {
                                Icon(
                                    imageVector = if (isMultiSelectMode) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isMultiSelectMode) "Done Selecting" else "Select Sets")
                            }

                            if (isMultiSelectMode) {
                                val allSelected = selectedSetIds.size == savedSets.size
                                FilledTonalButton(
                                    onClick = {
                                        selectedSetIds = if (allSelected) emptySet() else savedSets.map { it.id }.toSet()
                                    },
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("saved_sets_select_all_btn")
                                ) {
                                    Text(if (allSelected) "Deselect All" else "Select All (${savedSets.size})")
                                }

                                if (selectedSetIds.isNotEmpty()) {
                                    Button(
                                        onClick = { showBatchDeleteConfirm = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("saved_sets_batch_delete_btn")
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete (${selectedSetIds.size})")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = activeSection,
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 180)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 140))
                },
                label = "collections_section_transition",
                modifier = Modifier.weight(1f)
            ) { section ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (section == CollectionsTabSection.BOARDS) {
                        when (collectionsState) {
                            SyncState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            is SyncState.Error -> {
                                item {
                                    EmptyStatePanel(
                                        title = "Could not load collections",
                                        subtitle = collectionsState.message
                                    )
                                }
                            }
                            is SyncState.Ready -> {
                                val cols = collectionsState.data
                                if (cols.isEmpty()) {
                                    item {
                                        EmptyStatePanel(
                                            title = "Create your first board",
                                            subtitle = "Group wallpapers by mood, phone case color, or workspace aesthetic.",
                                            actionLabel = "New Collection",
                                            onAction = { showCreateDialog = true }
                                        )
                                    }
                                } else {
                                    val rows = cols.chunked(columns.coerceAtLeast(1))
                                    items(rows) { rowCols ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            rowCols.forEach { collection ->
                                                CollectionBoardCard(
                                                    collection = collection,
                                                    onOpen = { onOpenCollectionDetail(collection) },
                                                    onEdit = { editingCollection = collection },
                                                    onDelete = { onDeleteCollection(collection) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            repeat(columns.coerceAtLeast(1) - rowCols.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (section == CollectionsTabSection.OFFLINE) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("collections_offline_header"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${offlineWallpapers.size} Downloaded Canvases",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(
                                    onClick = { showInlineManagePack = !showInlineManagePack },
                                    modifier = Modifier.testTag("collections_inline_manage_pack_btn")
                                ) {
                                    Text(if (showInlineManagePack) "Hide Pack Manager" else "Manage Pack")
                                }
                            }
                        }
                        item {
                            AnimatedVisibility(visible = showInlineManagePack || offlineWallpapers.isEmpty()) {
                                OfflinePackSettingsCard(
                                    progress = offlinePackProgress,
                                    selectedOption = selectedOfflineOption,
                                    onSelectOption = { selectedOfflineOption = it },
                                    onStartOrResume = { onStartOfflinePack(selectedOfflineOption) },
                                    onPause = onPauseOfflinePack,
                                    onCancel = onCancelOfflinePack,
                                    onClearPack = onClearOfflinePack,
                                    modifier = Modifier.testTag("collections_inline_offline_pack_card")
                                )
                            }
                        }
                        if (offlineWallpapers.isEmpty()) {
                            item {
                                EmptyStatePanel(
                                    title = "No offline wallpapers downloaded yet",
                                    subtitle = "Download an Offline Wallpaper Pack right above so your canvases stay ready without internet."
                                )
                            }
                        } else {
                            val gridCols = (columns.coerceAtLeast(1) * 2).coerceIn(2, 4)
                            val rows = offlineWallpapers.chunked(gridCols)
                            items(rows) { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { wp ->
                                        WallpaperGridCard(
                                            wallpaper = wp,
                                            isFavorite = wp.id in favoriteIds,
                                            onClick = { onOpenWallpaper(wp) },
                                            onFavoriteToggle = { onToggleFavorite(wp.id) },
                                            aspectRatio = 0.65f,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(gridCols - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        // Display-only Lock Screen Set Gallery (does not mutate active Studio edits)
                        item {
                            val savedSets = (lockScreenSetsState as? SyncState.Ready)?.data.orEmpty()
                            val galleryItems = remember(savedSets) {
                                buildDisplayGalleryItems(savedSets)
                            }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.testTag("collections_lock_set_gallery")
                            ) {
                                Column {
                                    Text(
                                        text = "Lock Screen Set Gallery",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Display-only showcase compositions (read-only preview)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    items(galleryItems, key = { it.id }) { spec ->
                                        LockScreenDisplayGalleryCard(
                                            spec = spec,
                                            onInspectReadOnly = { previewDisplaySetCard = spec }
                                        )
                                    }
                                }
                            }
                        }

                        when (lockScreenSetsState) {
                            SyncState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            is SyncState.Error -> {
                                item {
                                    EmptyStatePanel(
                                        title = "Could not load saved sets",
                                        subtitle = lockScreenSetsState.message
                                    )
                                }
                            }
                            is SyncState.Ready -> {
                                val sets = lockScreenSetsState.data
                                if (sets.isEmpty()) {
                                    item {
                                        EmptyStatePanel(
                                            title = "No Lock-Screen Sets yet",
                                            subtitle = "Design a clock & depth look in Depth Studio and tap Save Set to keep it here."
                                        )
                                    }
                                } else {
                                    val rows = sets.chunked(columns.coerceAtLeast(1))
                                    items(rows) { rowSets ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            rowSets.forEach { lockSet ->
                                                val isSelected = lockSet.id in selectedSetIds
                                                SavedLockScreenSetCard(
                                                    lockSet = lockSet,
                                                    isMultiSelectMode = isMultiSelectMode,
                                                    isSelected = isSelected,
                                                    onToggleSelected = {
                                                        selectedSetIds = if (isSelected) {
                                                            selectedSetIds - lockSet.id
                                                        } else {
                                                            selectedSetIds + lockSet.id
                                                        }
                                                    },
                                                    onOpen = {
                                                        if (isMultiSelectMode) {
                                                            selectedSetIds = if (isSelected) selectedSetIds - lockSet.id else selectedSetIds + lockSet.id
                                                        } else {
                                                            onOpenLockScreenSet(lockSet)
                                                        }
                                                    },
                                                    onToggleFavorite = { onToggleLockScreenSetFavorite(lockSet.id) },
                                                    onDelete = { pendingDeleteSingleSet = lockSet },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            repeat(columns.coerceAtLeast(1) - rowSets.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CollectionEditorDialog(
            title = "New Collection",
            initialName = "",
            initialDescription = "",
            confirmLabel = "Create",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, desc ->
                onCreateCollection(name, desc)
                showCreateDialog = false
            }
        )
    }

    editingCollection?.let { target ->
        CollectionEditorDialog(
            title = "Edit Collection",
            initialName = target.name,
            initialDescription = target.description,
            confirmLabel = "Save",
            onDismiss = { editingCollection = null },
            onConfirm = { name, desc ->
                onRenameCollection(target, name, desc)
                editingCollection = null
            }
        )
    }

    pendingDeleteSingleSet?.let { targetSet ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSingleSet = null },
            title = { Text("Delete Saved Set?") },
            text = { Text("Remove \"${targetSet.name}\" from your saved Lock-Screen Sets? This action cannot be undone.") },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSingleSet = null }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteLockScreenSet(targetSet)
                        selectedSetIds = selectedSetIds - targetSet.id
                        pendingDeleteSingleSet = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_single_set")
                ) {
                    Text("Delete")
                }
            }
        )
    }

    if (showBatchDeleteConfirm) {
        val allSets = (lockScreenSetsState as? SyncState.Ready)?.data.orEmpty()
        val idsToDelete = selectedSetIds
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("Delete ${idsToDelete.size} Saved Sets?") },
            text = { Text("Remove ${idsToDelete.size} selected Lock-Screen Sets permanently? Your wallpaper files are not affected.") },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onDeleteLockScreenSets != null) {
                            onDeleteLockScreenSets(idsToDelete)
                        } else {
                            allSets.filter { it.id in idsToDelete }.forEach(onDeleteLockScreenSet)
                        }
                        selectedSetIds = emptySet()
                        isMultiSelectMode = false
                        showBatchDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_batch_delete_sets")
                ) {
                    Text("Delete ${idsToDelete.size}")
                }
            }
        )
    }

    previewDisplaySetCard?.let { spec ->
        Dialog(onDismissRequest = { previewDisplaySetCard = null }) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = glassSpecularBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("collections_lock_gallery_readonly_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LockScreenDisplayGalleryCard(
                        spec = spec,
                        onInspectReadOnly = {},
                        cardWidthDp = 240f,
                        cardHeightDp = 420f
                    )
                    Text(
                        text = "${spec.title} • ${spec.subtitle}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Display-only gallery showcase — active Studio edits remain unchanged.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { previewDisplaySetCard = null }) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

private data class GalleryPreviewSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val timeLabel: String,
    val dateLabel: String,
    val wallpaper: Wallpaper?,
    val tintColor: Color
)

private fun buildDisplayGalleryItems(savedSets: List<LockScreenSetDoc>): List<GalleryPreviewSpec> {
    val fromSaved = savedSets.take(4).map { setDoc ->
        val wp = WallpaperCatalog.getById(setDoc.wallpaperId)
        val parsed = ClockDesignConfig.fromJsonString(setDoc.clockConfigJson)
        GalleryPreviewSpec(
            id = "saved_${setDoc.id}",
            title = setDoc.name,
            subtitle = parsed?.face?.label ?: "Studio Glass Lock",
            timeLabel = "09:41",
            dateLabel = "Tuesday, Nov 25",
            wallpaper = wp,
            tintColor = Color(0xFFF6F8FC)
        )
    }
    val depthWallpapers = WallpaperCatalog.allWallpapers.filter { it.supportsDepthEffect }.take(4)
    val curated = listOf(
        GalleryPreviewSpec(
            id = "curated_glass",
            title = "Studio Glass Depth",
            subtitle = "Colorless Refractive Numerals",
            timeLabel = "09:41",
            dateLabel = "Tue 25 • 21° Clear",
            wallpaper = depthWallpapers.getOrNull(0),
            tintColor = Color(0xFFF8FAFF)
        ),
        GalleryPreviewSpec(
            id = "curated_editorial",
            title = "Editorial Sky Stack",
            subtitle = "Depth Contour • Subtle Halo",
            timeLabel = "10:15",
            dateLabel = "Wed 26 • Crisp Horizon",
            wallpaper = depthWallpapers.getOrNull(1) ?: depthWallpapers.firstOrNull(),
            tintColor = Color(0xFFFFE6C8)
        ),
        GalleryPreviewSpec(
            id = "curated_glyph",
            title = "Glyph Crisp Lock",
            subtitle = "Compact Top Weather Strip",
            timeLabel = "20:08",
            dateLabel = "Night • 14° Calm",
            wallpaper = depthWallpapers.getOrNull(2) ?: depthWallpapers.firstOrNull(),
            tintColor = Color(0xFFD6EAFF)
        )
    )
    return (fromSaved + curated).distinctBy { it.id }.take(6)
}

@Composable
private fun LockScreenDisplayGalleryCard(
    spec: GalleryPreviewSpec,
    onInspectReadOnly: () -> Unit,
    cardWidthDp: Float = 168f,
    cardHeightDp: Float = 296f
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
        tonalElevation = 4.dp,
        modifier = Modifier
            .size(width = cardWidthDp.dp, height = cardHeightDp.dp)
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onInspectReadOnly)
            .testTag("lock_gallery_card_${spec.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF101520))) {
            if (spec.wallpaper != null) {
                WallpaperAsyncImage(
                    wallpaper = spec.wallpaper,
                    useFullRes = false,
                    contentDescription = spec.title,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.36f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.72f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp, start = 10.dp, end = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = spec.dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = spec.tintColor.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = spec.timeLabel,
                    fontSize = (cardWidthDp * 0.24f).sp,
                    fontWeight = FontWeight.Bold,
                    color = spec.tintColor,
                    maxLines = 1
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = spec.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = spec.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SavedLockScreenSetCard(
    lockSet: LockScreenSetDoc,
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelected: () -> Unit = {},
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiStyle = LocalUiStyleMode.current
    val reducedMotion = LocalReducedMotionFlag.current
    val haptics = rememberStudioHaptics()
    val wallpaper = remember(lockSet.wallpaperId) {
        WallpaperCatalog.getById(lockSet.wallpaperId)
    }
    val parsedClock = remember(lockSet.clockConfigJson) {
        ClockDesignConfig.fromJsonString(lockSet.clockConfigJson) ?: ClockDesignConfig()
    }
    val bookmarkScale by animateFloatAsState(
        targetValue = if (lockSet.isFavorite && !reducedMotion) 1.18f else 1f,
        animationSpec = AppMotion.emphasisSpring(uiStyle, reducedMotion),
        label = "set_bookmark_scale"
    )

    TactileSurface(
        containerColor = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onOpen)
            .testTag("lock_set_card_${lockSet.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelected() },
                    modifier = Modifier.testTag("lock_set_checkbox_${lockSet.id}")
                )
            }
            Box(
                modifier = Modifier
                    .size(width = 92.dp, height = 136.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (wallpaper != null) {
                    WallpaperAsyncImage(
                        wallpaper = wallpaper,
                        useFullRes = false,
                        contentDescription = lockSet.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = lockSet.name,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.40f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.28f)
                                )
                            )
                        )
                )
                val clockTint = parsedClock.customPrimaryArgb?.let { Color(it.toInt()) } ?: Color.White
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "09:41",
                        fontFamily = parsedClock.font.fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = clockTint.copy(alpha = parsedClock.opacity.coerceIn(0.65f, 1f)),
                        maxLines = 1
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = lockSet.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${parsedClock.face.label} • ${parsedClock.finish.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val filterLabel = runCatching { CanvasLookFilter.valueOf(lockSet.lookFilterName).label }
                    .getOrDefault("Original")
                Text(
                    text = "${parsedClock.font.label} • $filterLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpen,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("lock_set_open_${lockSet.id}")
                    ) {
                        Icon(Icons.Rounded.Layers, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Studio", style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(
                        onClick = {
                            haptics.performConfirm()
                            onToggleFavorite()
                        },
                        modifier = Modifier.testTag("lock_set_fav_${lockSet.id}")
                    ) {
                        Icon(
                            imageVector = if (lockSet.isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (lockSet.isFavorite) "Remove set bookmark" else "Bookmark set",
                            tint = if (lockSet.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.scale(bookmarkScale)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("lock_set_delete_${lockSet.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete saved set",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionBoardCard(
    collection: UserCollectionDoc,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previewWallpapers = remember(collection.wallpaperIds) {
        collection.wallpaperIds.take(3).mapNotNull { WallpaperCatalog.getById(it) }
    }
    TactileSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onOpen)
            .testTag("collection_card_${collection.id}")
    ) {
        Column {
            if (previewWallpapers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    previewWallpapers.forEach { wp ->
                        WallpaperAsyncImage(
                            wallpaper = wp,
                            useFullRes = false,
                            contentDescription = "Collection canvas",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Empty collection — save canvases from Detail",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = collection.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = buildString {
                        append("${collection.wallpaperIds.size} canvases")
                        if (collection.description.isNotBlank()) {
                            append(" • ${collection.description}")
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Rename collection")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete collection")
                    }
                }
            }
        }
    }
}

@Composable
fun CollectionDetailScreen(
    collection: UserCollectionDoc,
    favoriteIds: Set<String>,
    columns: Int,
    onBack: () -> Unit,
    onOpenWallpaper: (Wallpaper) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRemoveFromCollection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val wallpapers = remember(collection.wallpaperIds) {
        collection.wallpaperIds.mapNotNull { WallpaperCatalog.getById(it) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to collections",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = collection.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = collection.description.ifBlank { "${wallpapers.size} saved canvases" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (wallpapers.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    EmptyStatePanel(
                        title = "Collection is empty",
                        subtitle = "Open any wallpaper and tap Save to Collection to add canvases here."
                    )
                }
            }
        } else {
            val rows = wallpapers.chunked(columns)
            items(rows) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { wp ->
                        Box(modifier = Modifier.weight(1f)) {
                            WallpaperGridCard(
                                wallpaper = wp,
                                isFavorite = wp.id in favoriteIds,
                                onClick = { onOpenWallpaper(wp) },
                                onFavoriteToggle = { onToggleFavorite(wp.id) },
                                aspectRatio = 0.65f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                                    .size(36.dp)
                            ) {
                                IconButton(onClick = { onRemoveFromCollection(wp.id) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.RemoveCircleOutline,
                                        contentDescription = "Remove from collection",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionEditorDialog(
    title: String,
    initialName: String,
    initialDescription: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Collection Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), description.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
