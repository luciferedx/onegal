package com.onegal.gallery.ui.viewmodel

import android.app.Application
import android.content.IntentSender
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onegal.gallery.data.model.AlbumItem
import com.onegal.gallery.data.model.DateSection
import com.onegal.gallery.data.model.ExifDetails
import com.onegal.gallery.data.model.ExternalDriveInfo
import com.onegal.gallery.data.model.GalleryTab
import com.onegal.gallery.data.model.MediaItem
import com.onegal.gallery.data.model.SortOrder
import com.onegal.gallery.data.model.SpecialCollection
import com.onegal.gallery.data.model.StorageSource
import com.onegal.gallery.data.model.ThemePreference
import com.onegal.gallery.data.repository.ExternalDriveRepository
import com.onegal.gallery.data.repository.MediaStoreRepository
import com.onegal.gallery.system.UsbDriveEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class GalleryUiState(
    val hasMediaPermission: Boolean = false,
    val isLoading: Boolean = false,
    val activeTab: GalleryTab = GalleryTab.PICTURES,
    val themePreference: ThemePreference = ThemePreference.DARK_AMOLED,
    val picturesColumns: Int = 4,
    val albumsColumns: Int = 3,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val internalMedia: List<MediaItem> = emptyList(),
    val trashedMedia: List<MediaItem> = emptyList(),
    val internalAlbums: List<AlbumItem> = emptyList(),
    val externalDrives: List<ExternalDriveInfo> = emptyList(),
    val pendingUsbPromptDevice: String? = null,
    val openedAlbum: AlbumItem? = null,
    val openedCollection: SpecialCollection? = null,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val isMenuSheetOpen: Boolean = false,
    val isSettingsDialogOpen: Boolean = false,
    val viewerItems: List<MediaItem> = emptyList(),
    val viewerIndex: Int? = null,
    val currentExif: ExifDetails? = null,
    val autoPlayVideos: Boolean = true,
    val toastMessage: String? = null
) {
    val activeGridItems: List<MediaItem>
        get() {
            val base = when {
                openedAlbum != null -> {
                    val album = openedAlbum
                    if (album.source == StorageSource.EXTERNAL_SAF) {
                        externalDrives.flatMap { it.items }.filter { it.bucketId == album.id }
                    } else {
                        internalMedia.filter { it.bucketId == album.id }
                    }
                }
                openedCollection != null -> when (openedCollection) {
                    SpecialCollection.FAVORITES -> internalMedia.filter { it.isFavorite }
                    SpecialCollection.VIDEOS -> internalMedia.filter { it.isVideo }
                    SpecialCollection.TRASH -> trashedMedia
                }
                else -> internalMedia
            }
            val filtered = if (searchQuery.isBlank()) {
                base
            } else {
                val q = searchQuery.trim().lowercase()
                base.filter {
                    it.displayName.lowercase().contains(q) ||
                        it.bucketName.lowercase().contains(q) ||
                        it.relativePath.lowercase().contains(q) ||
                        it.mimeType.lowercase().contains(q)
                }
            }
            return when (sortOrder) {
                SortOrder.DATE_DESC -> filtered.sortedByDescending { it.dateTakenMs }
                SortOrder.DATE_ASC -> filtered.sortedBy { it.dateTakenMs }
                SortOrder.NAME_ASC -> filtered.sortedBy { it.displayName.lowercase() }
                SortOrder.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
            }
        }
}

sealed interface PendingSystemRequest {
    data class LaunchIntentSender(val sender: IntentSender, val onSuccessMessage: String) : PendingSystemRequest
    data class ShareUris(val uris: List<Uri>, val mimeType: String) : PendingSystemRequest
}

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaStoreRepo = MediaStoreRepository(application)
    private val externalDriveRepo = ExternalDriveRepository(application)

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val _systemRequests = MutableSharedFlow<PendingSystemRequest>(extraBufferCapacity = 4)
    val systemRequests: SharedFlow<PendingSystemRequest> = _systemRequests.asSharedFlow()

    private var observerStarted = false

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasMediaPermission = granted) }
        if (granted) {
            refreshAll()
            startMediaObserver()
        }
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun startMediaObserver() {
        if (observerStarted) return
        observerStarted = true
        viewModelScope.launch {
            mediaStoreRepo.observeMediaChanges()
                .debounce(300L)
                .collect {
                    refreshInternalMedia()
                }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            refreshInternalMedia()
            refreshExternalDrives()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refreshInternalMedia() {
        viewModelScope.launch {
            val active = mediaStoreRepo.loadInternalMedia(includeTrashedOnly = false)
            val trashed = mediaStoreRepo.loadInternalMedia(includeTrashedOnly = true)
            val albums = mediaStoreRepo.buildAlbumsFromMedia(active)
            _uiState.update {
                it.copy(
                    internalMedia = active,
                    trashedMedia = trashed,
                    internalAlbums = albums
                )
            }
        }
    }

    fun refreshExternalDrives() {
        viewModelScope.launch {
            val drives = externalDriveRepo.scanAllMountedDrives()
            _uiState.update {
                it.copy(
                    externalDrives = drives,
                    pendingUsbPromptDevice = if (drives.isNotEmpty()) null else it.pendingUsbPromptDevice
                )
            }
        }
    }

    fun onUsbDriveEvent(event: UsbDriveEvent) {
        when (event) {
            is UsbDriveEvent.Attached -> {
                _uiState.update {
                    it.copy(
                        pendingUsbPromptDevice = event.deviceName,
                        toastMessage = "${event.deviceName} detected — scanning external folders"
                    )
                }
                refreshExternalDrives()
            }
            is UsbDriveEvent.MediaMounted -> {
                _uiState.update { it.copy(toastMessage = "External storage mounted") }
                refreshExternalDrives()
            }
            is UsbDriveEvent.Detached,
            is UsbDriveEvent.MediaUnmounted -> {
                _uiState.update { state ->
                    val closeExternalAlbum = state.openedAlbum?.source == StorageSource.EXTERNAL_SAF
                    state.copy(
                        openedAlbum = if (closeExternalAlbum) null else state.openedAlbum,
                        pendingUsbPromptDevice = null,
                        toastMessage = "External drive safely disconnected"
                    )
                }
                refreshExternalDrives()
            }
        }
    }

    fun onExternalTreeGranted(treeUri: Uri) {
        externalDriveRepo.persistTreePermission(treeUri)
        _uiState.update { it.copy(activeTab = GalleryTab.EXTERNAL, pendingUsbPromptDevice = null) }
        refreshExternalDrives()
    }

    fun disconnectSavedTree(treeUri: Uri) {
        externalDriveRepo.releaseTreePermission(treeUri)
        refreshExternalDrives()
    }

    fun selectTab(tab: GalleryTab) {
        if (tab == GalleryTab.MENU) {
            _uiState.update { it.copy(isMenuSheetOpen = true) }
        } else {
            _uiState.update {
                it.copy(
                    activeTab = tab,
                    openedAlbum = null,
                    openedCollection = null,
                    isSelectionMode = false,
                    selectedIds = emptySet()
                )
            }
        }
    }

    fun closeMenuSheet() = _uiState.update { it.copy(isMenuSheetOpen = false) }

    fun openSettings(open: Boolean) = _uiState.update {
        it.copy(isSettingsDialogOpen = open, isMenuSheetOpen = false)
    }

    fun setThemePreference(pref: ThemePreference) = _uiState.update { it.copy(themePreference = pref) }

    fun setAutoPlayVideos(enabled: Boolean) = _uiState.update { it.copy(autoPlayVideos = enabled) }

    fun setSortOrder(order: SortOrder) = _uiState.update { it.copy(sortOrder = order) }

    fun setPicturesColumns(columns: Int) = _uiState.update {
        it.copy(picturesColumns = columns.coerceIn(2, 5))
    }

    fun setAlbumsColumns(columns: Int) = _uiState.update {
        it.copy(albumsColumns = columns.coerceIn(2, 4))
    }

    fun toggleSearch(active: Boolean) = _uiState.update {
        it.copy(isSearchActive = active, searchQuery = if (!active) "" else it.searchQuery)
    }

    fun updateSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun openAlbum(album: AlbumItem) = _uiState.update {
        it.copy(openedAlbum = album, openedCollection = null, isSelectionMode = false, selectedIds = emptySet())
    }

    fun openSpecialCollection(collection: SpecialCollection) = _uiState.update {
        it.copy(
            openedCollection = collection,
            openedAlbum = null,
            isMenuSheetOpen = false,
            isSelectionMode = false,
            selectedIds = emptySet()
        )
    }

    fun navigateUp(): Boolean {
        val state = _uiState.value
        return when {
            state.viewerIndex != null -> {
                closeViewer()
                true
            }
            state.isSelectionMode -> {
                clearSelection()
                true
            }
            state.openedAlbum != null -> {
                _uiState.update { it.copy(openedAlbum = null) }
                true
            }
            state.openedCollection != null -> {
                _uiState.update { it.copy(openedCollection = null) }
                true
            }
            state.isSearchActive -> {
                toggleSearch(false)
                true
            }
            else -> false
        }
    }

    fun enterSelectionMode(initialId: String? = null) = _uiState.update {
        it.copy(
            isSelectionMode = true,
            selectedIds = if (initialId != null) setOf(initialId) else emptySet()
        )
    }

    fun toggleSelection(id: String) = _uiState.update { state ->
        val next = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
        state.copy(isSelectionMode = next.isNotEmpty(), selectedIds = next)
    }

    fun selectAll(items: List<MediaItem>) = _uiState.update { state ->
        val allIds = items.map { it.id }.toSet()
        val next = if (state.selectedIds.size == allIds.size) emptySet() else allIds
        state.copy(isSelectionMode = true, selectedIds = next)
    }

    fun clearSelection() = _uiState.update { it.copy(isSelectionMode = false, selectedIds = emptySet()) }

    fun openViewer(items: List<MediaItem>, index: Int) {
        val safeIndex = index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        _uiState.update { it.copy(viewerItems = items, viewerIndex = safeIndex, currentExif = null) }
        items.getOrNull(safeIndex)?.let { loadExifFor(it) }
    }

    fun onViewerPageChanged(index: Int) {
        val item = _uiState.value.viewerItems.getOrNull(index) ?: return
        _uiState.update { it.copy(viewerIndex = index) }
        loadExifFor(item)
    }

    fun closeViewer() = _uiState.update { it.copy(viewerIndex = null, currentExif = null) }

    private fun loadExifFor(item: MediaItem) {
        viewModelScope.launch {
            val details = mediaStoreRepo.readExifDetails(item)
            _uiState.update { it.copy(currentExif = details) }
        }
    }

    fun shareItems(items: List<MediaItem>) {
        if (items.isEmpty()) return
        val uris = items.map { it.uri }
        val allImages = items.all { !it.isVideo }
        val allVideos = items.all { it.isVideo }
        val mime = when {
            allImages -> "image/*"
            allVideos -> "video/*"
            else -> "*/*"
        }
        _systemRequests.tryEmit(PendingSystemRequest.ShareUris(uris, mime))
    }

    fun toggleFavorite(items: List<MediaItem>) {
        val internal = items.filter { it.source == StorageSource.INTERNAL_MEDIASTORE }
        if (internal.isEmpty()) {
            showToast("Favorites are stored on internal MediaStore items")
            return
        }
        val makeFav = internal.any { !it.isFavorite }
        val sender = mediaStoreRepo.createFavoriteIntentSender(internal.map { it.uri }, makeFav)
        _systemRequests.tryEmit(
            PendingSystemRequest.LaunchIntentSender(
                sender = sender,
                onSuccessMessage = if (makeFav) "Added to Favorites" else "Removed from Favorites"
            )
        )
        clearSelection()
    }

    fun trashOrDeleteItems(items: List<MediaItem>, permanent: Boolean = false) {
        viewModelScope.launch {
            val external = items.filter { it.source == StorageSource.EXTERNAL_SAF }
            val internal = items.filter { it.source == StorageSource.INTERNAL_MEDIASTORE }

            var externalDeleted = 0
            external.forEach { item ->
                if (externalDriveRepo.deleteExternalDocument(item)) externalDeleted++
            }
            if (externalDeleted > 0) {
                refreshExternalDrives()
                showToast("Deleted $externalDeleted file(s) from external drive")
            }

            if (internal.isNotEmpty()) {
                val uris = internal.map { it.uri }
                val sender = if (permanent) {
                    mediaStoreRepo.createDeleteIntentSender(uris)
                } else {
                    mediaStoreRepo.createTrashIntentSender(uris, trash = true)
                }
                _systemRequests.tryEmit(
                    PendingSystemRequest.LaunchIntentSender(
                        sender = sender,
                        onSuccessMessage = if (permanent) "Permanently deleted" else "Moved to Trash"
                    )
                )
            }
            clearSelection()
            if (_uiState.value.viewerIndex != null) closeViewer()
        }
    }

    fun restoreFromTrash(items: List<MediaItem>) {
        val uris = items.filter { it.source == StorageSource.INTERNAL_MEDIASTORE }.map { it.uri }
        if (uris.isEmpty()) return
        val sender = mediaStoreRepo.createTrashIntentSender(uris, trash = false)
        _systemRequests.tryEmit(
            PendingSystemRequest.LaunchIntentSender(
                sender = sender,
                onSuccessMessage = "Restored ${uris.size} item(s)"
            )
        )
        clearSelection()
    }

    fun renameItem(item: MediaItem, newName: String) {
        viewModelScope.launch {
            val cleaned = newName.trim()
            if (cleaned.isEmpty()) return@launch
            val success = if (item.source == StorageSource.EXTERNAL_SAF) {
                externalDriveRepo.renameExternalDocument(item, cleaned)
            } else {
                mediaStoreRepo.renameInternalMedia(item, cleaned)
            }
            if (success) {
                showToast("Renamed to $cleaned")
                refreshAll()
            } else if (item.source == StorageSource.INTERNAL_MEDIASTORE) {
                val sender = mediaStoreRepo.createWriteIntentSender(listOf(item.uri))
                _systemRequests.tryEmit(
                    PendingSystemRequest.LaunchIntentSender(sender, "Permission granted — retry rename")
                )
            } else {
                showToast("Unable to rename file")
            }
            clearSelection()
        }
    }

    fun copyOrMoveItems(
        items: List<MediaItem>,
        targetAlbum: AlbumItem,
        isMove: Boolean
    ) {
        viewModelScope.launch {
            var copied = 0
            val succeededSources = ArrayList<MediaItem>()
            for (item in items) {
                val destUri = if (targetAlbum.source == StorageSource.EXTERNAL_SAF &&
                    targetAlbum.treeUri != null && targetAlbum.documentId != null
                ) {
                    mediaStoreRepo.copyMediaToExternalFolder(item, targetAlbum.treeUri, targetAlbum.documentId)
                } else {
                    mediaStoreRepo.copyMediaToInternalAlbum(item, targetAlbum.relativePath)
                }
                if (destUri != null) {
                    copied++
                    succeededSources.add(item)
                }
            }
            if (isMove && succeededSources.isNotEmpty()) {
                trashOrDeleteItems(succeededSources, permanent = true)
            }
            showToast(
                if (isMove) "Moved $copied item(s) to ${targetAlbum.name}"
                else "Copied $copied item(s) to ${targetAlbum.name}"
            )
            refreshAll()
            clearSelection()
        }
    }

    fun captureVideoFrame(bitmap: Bitmap, videoName: String) {
        viewModelScope.launch {
            val saved = mediaStoreRepo.saveVideoFramePng(bitmap, videoName)
            showToast(
                if (saved != null) "Frame saved to Pictures/Onegal Captures"
                else "Failed to capture video frame"
            )
            if (saved != null) refreshInternalMedia()
        }
    }

    fun showToast(message: String) = _uiState.update { it.copy(toastMessage = message) }

    fun consumeToast() = _uiState.update { it.copy(toastMessage = null) }

    fun groupByDate(items: List<MediaItem>): List<DateSection> {
        if (items.isEmpty()) return emptyList()
        val nowCal = Calendar.getInstance()
        val todayKey = dayKeyFormat.format(nowCal.time)
        nowCal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayKey = dayKeyFormat.format(nowCal.time)

        val grouped = LinkedHashMap<String, MutableList<MediaItem>>()
        for (item in items) {
            val key = dayKeyFormat.format(Date(item.dateTakenMs))
            grouped.getOrPut(key) { ArrayList() }.add(item)
        }
        return grouped.map { (key, sectionItems) ->
            val sampleDate = Date(sectionItems.first().dateTakenMs)
            val title = when (key) {
                todayKey -> "Today"
                yesterdayKey -> "Yesterday"
                else -> sectionTitleFormat.format(sampleDate)
            }
            DateSection(
                key = key,
                title = title,
                monthBadge = monthBadgeFormat.format(sampleDate),
                items = sectionItems
            )
        }
    }

    companion object {
        private val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val sectionTitleFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
        private val monthBadgeFormat = SimpleDateFormat("MMM yyyy", Locale.US)
    }
}
