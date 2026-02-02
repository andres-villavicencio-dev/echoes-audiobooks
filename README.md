# Echoes 🎧

**Classic audiobooks & AI-generated stories** - beautifully narrated, freely accessible.

## Features

- 📚 **Classic Literature** - Public domain books with professional multi-voice narration
- ✨ **AI Stories** - Original short stories generated and narrated by AI
- 🎵 **Background Playback** - Listen while you work
- 💾 **Offline Downloads** - Save for listening anywhere
- 😴 **Sleep Timer** - Fall asleep to great stories
- ⚡ **Speed Control** - 0.5x to 2.0x playback
- 📑 **Chapter Navigation** - Jump to any chapter
- 🌙 **Dark Mode** - Easy on the eyes

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room
- **Network**: Retrofit + Kotlin Serialization
- **Audio**: Media3 (ExoPlayer)
- **Image Loading**: Coil
- **Async**: Coroutines + Flow

## Project Structure

```
app/src/main/java/com/echoesapp/audiobooks/
├── di/                 # Hilt modules
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # API interface, DTOs
│   └── repository/     # Repository implementations
├── domain/
│   ├── model/          # Domain models
│   └── usecase/        # Business logic (future)
├── player/             # Media3 playback service
└── ui/
    ├── components/     # Reusable composables
    ├── navigation/     # Nav graph
    ├── screens/        # Feature screens
    └── theme/          # Colors, typography
```

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Run tests
./gradlew test
```

## Content Generation

Audiobooks are generated using:
- **Orpheus TTS** - Multi-voice narration with emotion
- **VibeVoice** - Technical/documentary content
- **Chatterbox** - Voice cloning for specific narrators

## License

App code: MIT License
Content: Public domain classics + original AI-generated stories

---

*Made with 🦀 by Thelonious Crustaceous & Andus*
