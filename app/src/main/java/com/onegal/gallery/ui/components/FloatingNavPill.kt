package com.onegal.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onegal.gallery.data.model.GalleryTab
import com.onegal.gallery.ui.theme.OneUiColors

/**
 * Samsung One UI 8.0 Floating Translucent Pill Navigation Bar (28.dp corner radius).
 * Styled with #1C1C1E at 88% opacity and a 1.dp #2C2C2E hairline border in Dark Mode.
 */
@Composable
fun FloatingNavPill(
    activeTab: GalleryTab,
    isDarkTheme: Boolean,
    hasExternalDrive: Boolean,
    onTabSelected: (GalleryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillBg = if (isDarkTheme) {
        OneUiColors.DarkCard.copy(alpha = 0.88f)
    } else {
        Color.White.copy(alpha = 0.92f)
    }
    val pillBorder = if (isDarkTheme) {
        OneUiColors.DarkPillBorder
    } else {
        OneUiColors.LightBorder
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = pillBg,
            border = BorderStroke(1.dp, pillBorder),
            tonalElevation = 0.dp,
            shadowElevation = if (isDarkTheme) 12.dp else 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GalleryTab.entries.forEach { tab ->
                    val selected = tab == activeTab && tab != GalleryTab.MENU
                    val tint by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "navTint"
                    )
                    val icon = when (tab) {
                        GalleryTab.PICTURES -> if (selected) Icons.Rounded.PhotoLibrary else Icons.Outlined.PhotoLibrary
                        GalleryTab.ALBUMS -> if (selected) Icons.Rounded.PhotoAlbum else Icons.Outlined.PhotoAlbum
                        GalleryTab.EXTERNAL -> if (selected) Icons.Rounded.Usb else Icons.Outlined.Usb
                        GalleryTab.MENU -> Icons.Rounded.Menu
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(tab) }
                            )
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.label,
                            tint = if (tab == GalleryTab.EXTERNAL && hasExternalDrive && !selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                tint
                            },
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Multi-select bottom action bar replacing the floating nav pill when items are selected.
 */
@Composable
fun MultiSelectActionBar(
    isDarkTheme: Boolean,
    isTrashView: Boolean,
    selectedCount: Int,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barBg = if (isDarkTheme) OneUiColors.DarkCard.copy(alpha = 0.94f) else Color.White.copy(alpha = 0.96f)
    val border = if (isDarkTheme) OneUiColors.DarkPillBorder else OneUiColors.LightBorder
    val enabled = selectedCount > 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = barBg,
            border = BorderStroke(1.dp, border),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isTrashView) {
                    MultiSelectActionItem(
                        icon = Icons.Rounded.RestoreFromTrash,
                        label = "Restore",
                        enabled = enabled,
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onRestore,
                        modifier = Modifier.weight(1f)
                    )
                    MultiSelectActionItem(
                        icon = Icons.Rounded.DeleteOutline,
                        label = "Delete",
                        enabled = enabled,
                        tint = OneUiColors.OneUiDanger,
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    MultiSelectActionItem(Icons.Rounded.Share, "Share", enabled, onClick = onShare, modifier = Modifier.weight(1f))
                    MultiSelectActionItem(Icons.Rounded.ContentCopy, "Copy", enabled, onClick = onCopy, modifier = Modifier.weight(1f))
                    MultiSelectActionItem(Icons.Rounded.DriveFileMove, "Move", enabled, onClick = onMove, modifier = Modifier.weight(1f))
                    if (selectedCount == 1) {
                        MultiSelectActionItem(Icons.Rounded.DriveFileRenameOutline, "Rename", true, onClick = onRename, modifier = Modifier.weight(1f))
                    } else {
                        MultiSelectActionItem(Icons.Rounded.FavoriteBorder, "Favorite", enabled, onClick = onFavorite, modifier = Modifier.weight(1f))
                    }
                    MultiSelectActionItem(
                        icon = Icons.Rounded.DeleteOutline,
                        label = "Delete",
                        enabled = enabled,
                        tint = OneUiColors.OneUiDanger,
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiSelectActionItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTint = if (enabled) tint else tint.copy(alpha = 0.35f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = activeTint,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = activeTint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
