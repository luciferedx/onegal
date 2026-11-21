package com.onegal.gallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.onegal.gallery.data.model.DateSection
import com.onegal.gallery.data.model.MediaItem
import com.onegal.gallery.ui.theme.OneUiColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 120Hz Pinch-to-Zoom Media Grid with One UI Fast-Scroll Date Thumb.
 * Two-finger pinch transitions smoothly across 2, 3, 4, and 5 columns with haptic ticks.
 */
@Composable
fun MediaGrid(
    sections: List<DateSection>,
    allItemsFlat: List<MediaItem>,
    columns: Int,
    onColumnsChanged: (Int) -> Unit,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    onItemClick: (MediaItem, Int) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState()
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Flat index map for instant O(1) viewer lookup and fast-scroll badge resolution
    val flatIndexById = remember(allItemsFlat) {
        allItemsFlat.withIndex().associate { it.value.id to it.index }
    }
    val rowIndexToMonthBadge = remember(sections) {
        val list = ArrayList<String>()
        for (section in sections) {
            list.add(section.monthBadge)
            repeat(section.items.size) { list.add(section.monthBadge) }
        }
        list
    }

    // Pinch-to-zoom detector for 2..5 column grid transitions
    val pinchModifier = Modifier.pointerInput(columns) {
        var cumulativeZoom = 1f
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            cumulativeZoom = 1f
            do {
                val event = awaitPointerEvent()
                if (event.changes.count { it.pressed } >= 2) {
                    val zoom = event.calculateZoom()
                    if (zoom != 1f) {
                        cumulativeZoom *= zoom
                        if (cumulativeZoom > 1.24f && columns > 2) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onColumnsChanged((columns - 1).coerceIn(2, 5))
                            cumulativeZoom = 1f
                        } else if (cumulativeZoom < 0.80f && columns < 5) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onColumnsChanged((columns + 1).coerceIn(2, 5))
                            cumulativeZoom = 1f
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            contentPadding = PaddingValues(
                start = 2.dp,
                end = 2.dp,
                top = 4.dp,
                bottom = 112.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxSize()
                .then(pinchModifier)
        ) {
            sections.forEach { section ->
                item(
                    key = "hdr_${section.key}",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "date_header"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${section.items.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                itemsIndexed(
                    items = section.items,
                    key = { _, item -> item.id },
                    contentType = { _, item -> item.type }
                ) { _, item ->
                    val flatIndex = flatIndexById[item.id] ?: 0
                    val isSelected = item.id in selectedIds
                    MediaThumbnailTile(
                        item = item,
                        columns = columns,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = { onItemClick(item, flatIndex) },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onItemLongClick(item)
                        }
                    )
                }
            }
        }

        if (rowIndexToMonthBadge.size > 16) {
            FastScrollDateThumb(
                gridState = gridState,
                totalItems = rowIndexToMonthBadge.size,
                dateBadgeForIndex = { idx ->
                    rowIndexToMonthBadge.getOrElse(idx.coerceIn(0, rowIndexToMonthBadge.lastIndex)) { "" }
                },
                onScrollToItem = { target ->
                    scope.launch {
                        gridState.scrollToItem(target.coerceIn(0, rowIndexToMonthBadge.lastIndex))
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = 12.dp, bottom = 104.dp)
            )
        }
    }
}

@Composable
private fun MediaThumbnailTile(
    item: MediaItem,
    columns: Int,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val thumbPx = remember(columns) {
        when (columns) {
            2 -> 540
            3 -> 380
            4 -> 300
            else -> 240
        }
    }
    val imageRequest = remember(item.uri, thumbPx) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(Size(thumbPx, thumbPx))
            .crossfade(100)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(if (columns <= 2) 10.dp else 4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isSelected) 6.dp else 0.dp)
                .clip(RoundedCornerShape(if (isSelected) 12.dp else 0.dp))
        )

        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                if (item.durationMs > 0L) {
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = formatDurationMs(item.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }

        if (item.isFavorite && !isSelectionMode) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = "Favorite",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(14.dp)
            )
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) OneUiColors.OneUiBlue else Color.Black.copy(alpha = 0.42f)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) OneUiColors.OneUiBlue else Color.White.copy(alpha = 0.85f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FastScrollDateThumb(
    gridState: LazyGridState,
    totalItems: Int,
    dateBadgeForIndex: (Int) -> String,
    onScrollToItem: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val scrollFraction by remember(totalItems) {
        derivedStateOf {
            if (totalItems <= 1) 0f
            else (gridState.firstVisibleItemIndex.toFloat() / (totalItems - 1).toFloat()).coerceIn(0f, 1f)
        }
    }
    val activeFraction = if (isDragging) dragFraction else scrollFraction
    val activeIndex = (activeFraction * (totalItems - 1)).roundToInt().coerceIn(0, (totalItems - 1).coerceAtLeast(0))
    val activeBadge = dateBadgeForIndex(activeIndex)
    val showThumb = isDragging || gridState.isScrollInProgress

    BoxWithConstraints(modifier = modifier.width(128.dp)) {
        val density = LocalDensity.current
        val trackHeightPx = with(density) { maxHeight.toPx() - 48.dp.toPx() }.coerceAtLeast(1f)
        val thumbOffsetY = (activeFraction * trackHeightPx).roundToInt()

        AnimatedVisibility(
            visible = showThumb,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, thumbOffsetY) }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                if (isDragging && activeBadge.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = OneUiColors.DarkCard.copy(alpha = 0.94f),
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = activeBadge,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isDragging) OneUiColors.OneUiBlue else OneUiColors.DarkCard.copy(alpha = 0.88f),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(width = 30.dp, height = 46.dp)
                        .pointerInput(totalItems, trackHeightPx) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    dragFraction = scrollFraction
                                },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragFraction = (dragFraction + dragAmount / trackHeightPx).coerceIn(0f, 1f)
                                    val target = (dragFraction * (totalItems - 1)).roundToInt()
                                    onScrollToItem(target)
                                }
                            )
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.UnfoldMore,
                            contentDescription = "Fast scroll",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatDurationMs(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
