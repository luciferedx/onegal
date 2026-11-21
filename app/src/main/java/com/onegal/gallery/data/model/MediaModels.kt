package com.onegal.gallery.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable

enum class MediaType {
    IMAGE,
    VIDEO
}

enum class StorageSource {
    INTERNAL_MEDIASTORE,
    EXTERNAL_SAF
}

enum class GalleryTab(val label: String) {
    PICTURES("Pictures"),
    ALBUMS("Albums"),
    EXTERNAL("External"),
    MENU("Menu")
}

enum class SortOrder(val label: String) {
    DATE_DESC("Newest first"),
    DATE_ASC("Oldest first"),
    NAME_ASC("Name (A–Z)"),
    SIZE_DESC("Largest first")
}

enum class ThemePreference(val label: String) {
    DARK_AMOLED("AMOLED Pitch Black"),
    LIGHT("One UI Light"),
    SYSTEM("Follow System")
}

enum class SpecialCollection(val title: String) {
    FAVORITES("Favorites"),
    VIDEOS("Videos"),
    TRASH("Trash")
}

@Immutable
data class MediaItem(
    val id: String,
    val mediaStoreId: Long? = null,
    val uri: Uri,
    val displayName: String,
    val dateTakenMs: Long,
    val dateModifiedSec: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long = 0L,
    val mimeType: String,
    val bucketId: String,
    val bucketName: String,
    val relativePath: String,
    val source: StorageSource,
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
    val treeUri: Uri? = null,
    val documentId: String? = null
) {
    val type: MediaType
        get() = if (mimeType.startsWith("video/", ignoreCase = true)) MediaType.VIDEO else MediaType.IMAGE

    val isVideo: Boolean
        get() = type == MediaType.VIDEO
}

@Immutable
data class AlbumItem(
    val id: String,
    val name: String,
    val coverItem: MediaItem,
    val itemCount: Int,
    val totalSizeBytes: Long,
    val source: StorageSource,
    val relativePath: String,
    val treeUri: Uri? = null,
    val documentId: String? = null
)

@Immutable
data class ExternalDriveInfo(
    val treeUri: Uri,
    val label: String,
    val rootDocumentId: String,
    val folders: List<AlbumItem>,
    val items: List<MediaItem>,
    val totalBytes: Long,
    val scanTimeMs: Long
)

@Immutable
data class ExifDetails(
    val cameraMake: String?,
    val cameraModel: String?,
    val aperture: String?,
    val exposureTime: String?,
    val iso: String?,
    val focalLength: String?,
    val lensModel: String?,
    val dateTimeOriginal: String?,
    val gpsCoordinates: String?,
    val width: Int,
    val height: Int
)

@Immutable
data class DateSection(
    val key: String,
    val title: String,
    val monthBadge: String,
    val items: List<MediaItem>
)
