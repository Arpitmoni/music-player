

---

##  Project Structure

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



*Built with ❤️ using Kotlin + ExoPlayer + Material 3*
