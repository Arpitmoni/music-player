# 🎵 Pulse — Modern Android Music Player

A premium, production-quality music player for Android built with **Kotlin**, **ExoPlayer (Media3)**, **MVVM architecture**, and **Material 3 dark theme**.

---

## ✨ Features

| Feature | Details |
|---|---|
| 🎨 UI | Dark theme, purple/pink gradients, glassmorphism cards |
| 🎵 Playback | ExoPlayer (Media3) with smooth gapless playback |
| 📂 Library | Loads all device songs via MediaStore |
| 🔔 Notification | Persistent media notification with transport controls |
| 🏗️ Architecture | MVVM + Repository + LiveData + Coroutines |
| 🔄 Background | Foreground Service keeps music playing when app is closed |
| 🖼️ Album Art | Glide-powered lazy loading with fade-in transitions |
| 🎛️ Mini Player | Slide-up bottom bar with play/pause + next |
| 📺 Now Playing | Full-screen player with animated album art |
| ⏩ Seek | Smooth seekbar with live mm:ss position labels |

---

## 📁 Project Structure

```
MusicPlayer/
├── app/
│   ├── build.gradle                          ← Dependencies
│   └── src/main/
│       ├── AndroidManifest.xml               ← Permissions + Service declaration
│       ├── java/com/musicplayer/
│       │   ├── MusicPlayerApp.kt             ← Application class
│       │   ├── data/
│       │   │   ├── model/
│       │   │   │   ├── Song.kt               ← Data class for a track
│       │   │   │   └── PlayerState.kt        ← Sealed class: Idle/Loading/Playing/Paused/Error
│       │   │   └── repository/
│       │   │       └── SongRepository.kt     ← MediaStore queries (IO coroutine)
│       │   ├── service/
│       │   │   ├── MusicService.kt           ← Foreground service wrapping ExoPlayer
│       │   │   └── PlayerController.kt       ← Singleton bridging service ↔ ViewModels
│       │   └── ui/
│       │       ├── adapter/
│       │       │   └── SongAdapter.kt        ← RecyclerView ListAdapter with DiffUtil
│       │       ├── home/
│       │       │   └── MainActivity.kt       ← Song list + mini-player
│       │       ├── player/
│       │       │   └── NowPlayingActivity.kt ← Full-screen player
│       │       ├── viewmodel/
│       │       │   ├── MainViewModel.kt      ← Home screen ViewModel
│       │       │   └── NowPlayingViewModel.kt← Player screen ViewModel
│       │       └── utils/
│       │           └── Extensions.kt         ← Animation helpers + time formatter
│       └── res/
│           ├── anim/                         ← slide_up, slide_down, fade_in, fade_out, scale_in
│           ├── drawable/                     ← Vector icons + shape backgrounds
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── activity_now_playing.xml
│           │   ├── layout_mini_player.xml
│           │   └── item_song.xml
│           └── values/
│               ├── colors.xml
│               ├── strings.xml
│               ├── themes.xml
│               └── dimens.xml
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 26+
- Kotlin 1.9+
- A physical Android device or emulator running Android 8.0+

### 1. Clone / Open project
```bash
# Option A: copy the project folder into Android Studio
File → Open → select the MusicPlayer/ folder

# Option B: create a new project and replace files manually
```

### 2. Sync Gradle
Android Studio will prompt you to sync. Click **Sync Now**.  
If you see a Gradle version mismatch, update `gradle/wrapper/gradle-wrapper.properties`:
```
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
```

### 3. Run on device
Connect a physical Android device (recommended — emulators don't have music files).  
Click ▶ **Run**.

---

## 🔑 Permissions Explained

| Permission | Why needed |
|---|---|
| `READ_MEDIA_AUDIO` (API 33+) | Scan device music library |
| `READ_EXTERNAL_STORAGE` (≤API 32) | Same, for older Android |
| `FOREGROUND_SERVICE` | Keep playback alive in background |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Required by Android 14+ for audio foreground services |
| `WAKE_LOCK` | Keep CPU awake during playback |
| `POST_NOTIFICATIONS` | Show media notification (Android 13+) |

Permissions are requested **at runtime** in `MainActivity.kt` — the app gracefully shows an error message if denied.

---

## 🏗️ Architecture Deep Dive

```
┌──────────────────────────────────────────────┐
│                   UI Layer                    │
│  MainActivity  ←→  MainViewModel             │
│  NowPlayingActivity ←→ NowPlayingViewModel   │
│  SongAdapter (RecyclerView)                  │
└────────────────┬─────────────────────────────┘
                 │ observes LiveData
┌────────────────▼─────────────────────────────┐
│              PlayerController                 │
│         (Singleton — bridges layers)          │
│  exposes: playerState: LiveData<PlayerState> │
│           progress: LiveData<Pair<Long,Long>> │
└────────────────┬─────────────────────────────┘
                 │ binds to
┌────────────────▼─────────────────────────────┐
│              MusicService                     │
│        (Foreground Service)                   │
│  wraps ExoPlayer                             │
│  fires: onPlaybackStateChanged               │
│         onProgressChanged                    │
└────────────────┬─────────────────────────────┘
                 │ reads songs from
┌────────────────▼─────────────────────────────┐
│            SongRepository                     │
│       (queries MediaStore on IO thread)       │
└──────────────────────────────────────────────┘
```

**Key design decisions:**
- `PlayerController` is a **singleton** so both `MainActivity` and `NowPlayingActivity` share the same LiveData — the mini-player and full player are always in sync.
- `MusicService` uses `LifecycleService` so it can safely launch coroutines for progress polling.
- `SongAdapter` uses `ListAdapter` + `DiffUtil` for **O(n) diffing** — only changed rows are redrawn.
- The ViewModel uses `AndroidViewModel` (not plain `ViewModel`) so it can safely hold an `Application` reference for the repository.

---

## 🎨 UI/Design System

### Colour Palette
| Token | Hex | Usage |
|---|---|---|
| `bg_primary` | `#0D0D0D` | App background |
| `bg_surface` | `#1A1A2E` | Cards, surfaces |
| `accent_purple` | `#9B59B6` | Active states, icons |
| `accent_blue` | `#6C63FF` | Gradient start |
| `accent_pink` | `#E91E8C` | Gradient end |
| `text_primary` | `#F0F0F0` | Titles |
| `text_secondary` | `#9E9E9E` | Artist, subtitles |

### Animations
- **Song list fade-in** — `RecyclerView` fades in after data loads
- **Mini player slide-up** — slides from bottom when playback starts
- **Album art scale** — shrinks to 85% when paused, springs back to 100% when playing (with `animate().scaleX().scaleY()`)
- **Screen transitions** — slide-up to open player, slide-down to close

---

## ➕ How to Extend

### Add shuffle / repeat
```kotlin
// In MusicService
fun toggleShuffle() { player.shuffleModeEnabled = !player.shuffleModeEnabled }
fun toggleRepeat()  { player.repeatMode = if (player.repeatMode == Player.REPEAT_MODE_OFF)
                          Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF }
```

### Add search / filter
```kotlin
// In MainViewModel
fun filterSongs(query: String) {
    val all = _allSongs.value ?: return
    _songs.value = if (query.isBlank()) all
                   else all.filter { it.title.contains(query, ignoreCase = true) }
}
```

### Add Room database for playlists
1. Add `androidx.room:room-runtime:2.6.1` + `room-ktx` to `build.gradle`
2. Create `@Entity Playlist`, `@Dao PlaylistDao`, `@Database AppDatabase`
3. Inject via the repository layer

### Add album art colour extraction (Palette API)
```kotlin
Glide.with(ctx).asBitmap().load(song.albumArtUri)
    .into(object : CustomTarget<Bitmap>() {
        override fun onResourceReady(bmp: Bitmap, t: Transition<in Bitmap>?) {
            Palette.from(bmp).generate { palette ->
                val dominant = palette?.getDominantColor(Color.DKGRAY)
                // Apply dominant to background gradient
            }
        }
        override fun onLoadCleared(p: Placeholder?) {}
    })
```

---

## 🐛 Troubleshooting

| Problem | Solution |
|---|---|
| No songs appear | Grant storage permission in Settings → Apps → Pulse → Permissions |
| `ClassNotFoundException: MusicService` | Ensure service is declared in `AndroidManifest.xml` |
| Music stops after screen off | Check `WAKE_LOCK` permission + `startForeground()` is called |
| Notification doesn't show | Grant notification permission (Android 13+); check `CHANNEL_ID` is created |
| Album art is blank | Album art URI depends on device; fallback `placeholder_album` shows instead |
| Gradle sync fails | Check `compileSdk 34` matches your installed SDK; update via SDK Manager |

---

## 📦 Dependencies Summary

```
androidx.media3:media3-exoplayer:1.2.1      ← Core audio engine
androidx.media3:media3-session:1.2.1        ← MediaSession / notification integration
com.github.bumptech.glide:glide:4.16.0     ← Album art image loading
androidx.lifecycle:lifecycle-viewmodel-ktx  ← MVVM ViewModels
kotlinx-coroutines-android:1.7.3           ← Background threading
androidx.palette:palette-ktx:1.0.0         ← (ready for colour extraction)
com.google.android.material:material:1.11.0 ← Material 3 components
```

---

*Built with ❤️ using Kotlin + ExoPlayer + Material 3*
