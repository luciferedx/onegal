package com.onegal.gallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onegal.gallery.data.model.SortOrder

class OneUiCollapseState(
    val maxHeightPx: Float,
    val minHeightPx: Float
) {
    var heightOffsetPx by mutableFloatStateOf(0f)

    val collapseRangePx: Float
        get() = (maxHeightPx - minHeightPx).coerceAtLeast(1f)

    val collapsedFraction: Float
        get() = (-heightOffsetPx / collapseRangePx).coerceIn(0f, 1f)

    val currentHeightPx: Float
        get() = (maxHeightPx + heightOffsetPx).coerceIn(minHeightPx, maxHeightPx)

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            if (delta < 0f && heightOffsetPx > -collapseRangePx) {
                val previous = heightOffsetPx
                heightOffsetPx = (heightOffsetPx + delta).coerceIn(-collapseRangePx, 0f)
                return Offset(0f, heightOffsetPx - previous)
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            val delta = available.y
            if (delta > 0f && heightOffsetPx < 0f) {
                val previous = heightOffsetPx
                heightOffsetPx = (heightOffsetPx + delta).coerceIn(-collapseRangePx, 0f)
                return Offset(0f, heightOffsetPx - previous)
            }
            return Offset.Zero
        }
    }
}

@Composable
fun rememberOneUiCollapseState(
    expandedHeight: Dp = 216.dp,
    collapsedHeight: Dp = 56.dp
): OneUiCollapseState {
    val density = LocalDensity.current
    val maxPx = with(density) { expandedHeight.toPx() }
    val minPx = with(density) { collapsedHeight.toPx() }
    return remember(maxPx, minPx) { OneUiCollapseState(maxPx, minPx) }
}

/**
 * Samsung One UI 8.0 signature collapsible reachability header:
 * Positioned in the upper third when expanded, collapsing smoothly into a sticky compact bar.
 */
@Composable
fun OneUiCollapsibleHeader(
    title: String,
    subtitle: String,
    collapseState: OneUiCollapseState,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    isSelectionMode: Boolean,
    selectedCount: Int,
    totalSelectableCount: Int,
    onToggleSelectAll: () -> Unit,
    onCancelSelection: () -> Unit,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchToggle: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    currentColumns: Int,
    onCycleColumns: () -> Unit,
    currentSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
    onEnterSelection: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val density = LocalDensity.current
    val currentHeightDp = with(density) { collapseState.currentHeightPx.toDp() }
    val fraction = collapseState.collapsedFraction
    var moreExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        if (isSelectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleSelectAll) {
                    val allSelected = totalSelectableCount > 0 && selectedCount == totalSelectableCount
                    Icon(
                        imageVector = if (allSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = "Select all",
                        tint = if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (selectedCount == 0) "Select items" else "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCancelSelection) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cancel selection",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            return
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isSearchActive) 60.dp else currentHeightDp)
        ) {
            // Expanded reachability header centered in upper third
            if (!isSearchActive && fraction < 0.92f) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .alpha((1f - fraction * 1.45f).coerceIn(0f, 1f)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Compact sticky top bar anchored at bottom of collapsing box
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Spacer(Modifier.width(8.dp))
                }

                if (isSearchActive) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search filename, folder, or type",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                inner()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    IconButton(onClick = { onSearchToggle(false) }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close search",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    AnimatedVisibility(
                        visible = fraction > 0.55f || showBackButton,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!(fraction > 0.55f || showBackButton)) {
                        Spacer(Modifier.weight(1f))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onSearchToggle(true) }) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = onEnterSelection) {
                            Icon(
                                imageVector = Icons.Rounded.SelectAll,
                                contentDescription = "Select",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = onCycleColumns) {
                            Icon(
                                imageVector = Icons.Rounded.GridView,
                                contentDescription = "View density ($currentColumns columns)",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Box {
                            IconButton(onClick = { moreExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            DropdownMenu(
                                expanded = moreExpanded,
                                onDismissRequest = { moreExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (order == currentSort) "${order.label}  •" else order.label,
                                                color = if (order == currentSort) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        },
                                        onClick = {
                                            onSortSelected(order)
                                            moreExpanded = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Settings", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        moreExpanded = false
                                        onOpenSettings()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
