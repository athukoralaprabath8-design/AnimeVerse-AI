# AnimeVerse AI

An early Android prototype for anime-inspired, user-directed roleplay.

## Current prototype
- Jetpack Compose Android app
- Three selectable character labels and editable story-world field
- Local chat interface with placeholder scripted replies
- GitHub Actions workflow to build and upload a debug APK

## Build
The GitHub Actions workflow runs on pushes to `main` and can also be started manually from **Actions → Build AnimeVerse AI APK → Run workflow**. Download the `AnimeVerse-AI-debug` artifact after the run succeeds.

For a local build, install JDK 17, Android SDK (platform 35), and Gradle 8.9, then run:

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Important
This is a starter prototype, not the finished AI product. Chat replies are scripted and stored only in app memory; there is no connected AI model, account system, durable memory, image/video generation, or backend yet. A live AI service requires a provider, secure server-side API-key handling, privacy controls, and abuse safeguards. Do not put secret API keys in the Android app or GitHub source.
