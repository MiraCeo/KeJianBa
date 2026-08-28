package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp

/** Forum images always use the shared squircle; account/user avatars use Avatar. */
@Composable
fun ForumAvatar(
    data: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) = Avatar(
    data = data,
    size = size,
    modifier = modifier,
    contentDescription = contentDescription,
    shape = ForumAvatarShape,
    contentScale = ContentScale.Fit,
)

@Composable
fun ForumAvatar(
    data: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) = Avatar(
    data = data,
    modifier = modifier,
    contentDescription = contentDescription,
    shape = ForumAvatarShape,
    contentScale = ContentScale.Fit,
)
