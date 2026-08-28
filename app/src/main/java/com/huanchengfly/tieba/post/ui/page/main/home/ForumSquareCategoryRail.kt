package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.page.main.MainCardStyle

internal val ForumSquareRailWidth = 68.dp
internal val ForumSquareCategoryMinHeight = 48.dp

// Round the grey rows adjacent to the selection, not the white selected row.
// End corners also mirror correctly in RTL layouts.
internal fun forumSquareCategoryShape(index: Int, selectedIndex: Int): RoundedCornerShape =
    RoundedCornerShape(
        topEnd = if (selectedIndex >= 0 && index == selectedIndex + 1) 10.dp else 0.dp,
        bottomEnd = if (selectedIndex >= 0 && index == selectedIndex - 1) 10.dp else 0.dp,
    )

@Composable
internal fun ForumSquareCategoryRail(
    categories: List<String>,
    selectedCategory: String,
    surfaceColor: Color,
    onSelect: (String) -> Unit,
) {
    val selectedIndex = categories.indexOf(selectedCategory)
    val railColor = if (MainCardStyle.enabled) Color(0xFFF2F2F4) else MaterialTheme.colorScheme.surfaceContainerLow
    LazyColumn(
        modifier = Modifier.width(ForumSquareRailWidth).fillMaxHeight()
            .background(railColor).selectableGroup(),
    ) {
        itemsIndexed(categories, key = { _, category -> category }) { index, category ->
            val selected = category == selectedCategory
            val fill by animateColorAsState(
                if (selected) surfaceColor else railColor,
                animationSpec = tween(150), label = "ForumCategoryFill",
            )
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(surfaceColor)
                    .clip(forumSquareCategoryShape(index, selectedIndex))
                    .background(fill)
                    .selectable(
                        selected = selected,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(category) },
                    )
                    .heightIn(min = ForumSquareCategoryMinHeight)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
