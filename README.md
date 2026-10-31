# Playlog

A local-first game diary for Android, in the spirit of Letterboxd but built from scratch for games.
Original code, original branding, and no assets or code from the Letterboxd APK in this workspace.

## Features

- **Discover / Search** — browse popular games or search the IGDB catalog.
- **Game pages** — cover, backdrop, IGDB rating, genres, platforms, summary, screenshots.
- **Library** — Playing / Played / Backlog / Dropped / Liked, with per-game ratings.
- **Diary** — timestamped entries with 0.5–5 star ratings, reviews, and replay flags.
- **Lists** — private curated lists, add/remove games.
- **Profile** — counts, average rating, rating spread, top genres, favourites, recent activity.
- **Offline-first** — all personal data is stored on-device as JSON; no account, no backend.

## IGDB setup

IGDB uses Twitch OAuth. Create a free application at <https://dev.twitch.tv/console/apps>,
then put the credentials in `local.properties` (already ignored by git):

```properties
IGDB_CLIENT_ID=your_client_id
IGDB_CLIENT_SECRET=your_client_secret
```

Without credentials the app runs in **demo mode** with a bundled 16-game catalog.
The Settings screen shows which mode is active.

> For a personal build, compiling the client secret into `BuildConfig` is acceptable.
> Before distributing the APK, move the token exchange to a small backend proxy.

## Build

Requirements: JDK 17+ and Android SDK 36.

```powershell
# from this directory
.\gradlew.bat :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

The checked-in `local.properties` points at `C:\Android\Sdk`; change `sdk.dir` for another machine.

## Stack

- Kotlin 2.2 + Jetpack Compose (Material 3)
- Navigation Compose
- OkHttp + kotlinx.serialization (IGDB v4)
- Coil (image loading)
- Single-activity MVVM, no DI framework

## Project layout

```
app/src/main/java/com/playlog/app/
  data/        models, JSON store, IGDB client, repository, demo catalog
  ui/          navigation, theme, components, screens
  PlaylogViewModel.kt
  MainActivity.kt
```

## Legal

Playlog is an independent project. It is not affiliated with Letterboxd, IGDB, or Twitch.
Game metadata comes from IGDB. Cover art is served by IGDB's image CDN.
