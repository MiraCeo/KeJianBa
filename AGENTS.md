# UI conventions

- Render Tieba forum avatars with `ui/widgets/compose/ForumAvatar` and its shared `ForumAvatarShape` (squircle with gently bowed sides). Use the same shape for forum-avatar placeholders. Do not substitute ordinary rounded rectangles or circles.
- Keep account, author, and other user avatars separate: they continue using `Avatar` and their existing styles.
- The Home "吧广场" tab is intentionally blank until explicitly requested; do not add network requests, categories, or placeholder features to it.
