# UI conventions

- Render Tieba forum avatars with `ui/widgets/compose/ForumAvatar` and its shared `ForumAvatarShape` (squircle with gently bowed sides). Use the same shape for forum-avatar placeholders. Do not substitute ordinary rounded rectangles or circles.
- Keep account, author, and other user avatars separate: they continue using `Avatar` and their existing styles.
- The Home "吧广场" tab is implemented and live — see the Forum Square section below before changing it.

# Forum Square (吧广场)

The Home "吧广场" tab is **fully implemented and shipping**. It is page 1 of the
Home pager (`ui/page/main/home/HomePage.kt`) and consists of
`ForumSquarePage.kt`, `ForumSquareViewModel.kt`, `ForumSquareCategoryRail.kt`,
`repository/ForumSquareRepository`, `protos/ForumSquare/ForumSquare.proto`, and
unit tests under `ui/page/main/ForumSquareTest.kt`,
`ForumSquareRecommendationTest.kt`, and `home/ForumSquareCategoryRailTest.kt`.

An earlier revision of this file described the tab as "intentionally blank".
That is obsolete: the tab has a category rail, live network data, follow
buttons, pull-to-refresh, and endless scrolling. Do not blank it, stub it, or
strip its network calls.

Preserve these invariants when touching it:

- **Off-screen composition must not fetch.** The Home pager keeps the square
  composed with `beyondViewportPageCount = 1`, so the page exists before it is
  selected. Fetching is gated on `isActive`, threaded through
  `activate(uid, isActive)` / `deactivate()`. Never move a request into
  composition or a bare `LaunchedEffect(Unit)`.
- **`generation` tokens guard against races.** Switching category or account
  bumps `generation` and cancels the in-flight job; late responses are dropped
  by comparing the token. A different account resets the whole state.
- **`paused` is deliberate, not a bug.** When the server still reports
  `hasMore` but a page contributes no new forums, automatic load-more stops and
  waits for an explicit user action. Do not "fix" this into an auto-retry loop:
  it is what prevents infinite paging on duplicate pages.
- **Responses are validated.** A mismatch between the requested and returned
  category or page raises `IOException`. Keep that check.
- Forum rows use `ForumAvatar` per the avatar convention above.

# Agent environment (MCP mode)

These notes apply when an agent drives this repo **through the MCP bridge** —
remote tools (`run_command`, `read_files`, `apply_patch`) acting on the Windows
host — rather than running locally with direct filesystem access. In MCP mode the
agent only ever receives UTF-8 text back, so any binary file needs an explicit
transfer step. None of this is necessary for a local agent that can open files
on disk directly.

## Viewing device screenshots

`read_files` cannot return binary data, so a PNG pulled off the device is
unreadable as-is. Re-encode it to a compact JPEG on the host, ship it as base64
text, and decode it on the agent side:

1. `adb shell screencap -p /sdcard/__s.png`, `adb pull` it into `$env:TEMP`, then
   `adb shell rm` the copy on the device.
2. Downscale and re-encode with `System.Drawing`: JPEG, ~540 px wide, quality 70.
3. `[Convert]::ToBase64String(...)`, emitted with `[Console]::Out.Write` so
   PowerShell does not line-wrap the payload.
4. Read the result back in pages via `get_command_output`, strip whitespace,
   decode, and write it out as a real `.jpg`.

Reference cost: a 1440x3168 screen becomes a ~69 KB JPEG / ~92 KB of base64 and
arrives in two pages within a few seconds. Use ~540 px to review layout and
navigation; use 1080 px when pixel-level detail matters, such as checking a
divider colour or spacing (roughly 3-4x the bytes).

The same base64 round-trip moves any binary artifact off the host: logs, APKs,
traces, PDFs.

Never redirect binary output through PowerShell `>` — for example
`adb exec-out screencap -p > file.png` corrupts the stream. Always `screencap`
to a device path and `adb pull` it.

## Installing on the test device

`adb` is not on `PATH`; invoke it as `D:\Android\Sdk\platform-tools\adb.exe`.

Plain `adb install` fails on the ColorOS test device with `Failure [-99]`, even
with `verifier_verify_adb_installs` set to 0, because the OEM intercepts the
streamed-install path. Push the APK first and install it from the device side:

    adb push app-debug.apk /data/local/tmp/x.apk
    adb shell pm install -r -t /data/local/tmp/x.apk

Debug builds carry the `.debug` applicationId suffix, so they install alongside
the release build (`可简吧`) instead of colliding with its signature. The two keep
separate data, so the debug build needs its own login.
