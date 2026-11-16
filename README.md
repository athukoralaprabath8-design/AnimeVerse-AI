# AnimeVerse AI

AnimeVerse AI is an original Android anime-world roleplay app prototype. The goal is a living visual-novel experience with memorable characters, evolving relationships, branching stories, quests, and user-created worlds.

## Current build: v0.1.0

- Jetpack Compose Android interface with a dark anime-inspired theme.
- Three sample characters and support for creating additional character profiles.
- Editable story-world name and selectable active character.
- Local persistence for the cast, conversation history (up to 250 messages), chapter count, romance setting, and bond points.
- Online AI chat through OpenRouter's OpenAI-compatible Chat Completions API, using the `openrouter/free` model router.
- 18+ self-attestation setting for mature romance themes; romantic/adult scenarios are restricted by the app prompt to clearly adult, consenting characters.
- GitHub Actions workflow builds and uploads a debug APK.

## Install the latest debug APK

1. Open [GitHub Actions](https://github.com/athukoralaprabath8-design/AnimeVerse-AI/actions).
2. Open the latest successful **Build AnimeVerse AI APK** run.
3. Download the `AnimeVerse-AI-debug` artifact ZIP.
4. Extract it on Android and install the included APK. Android may ask you to allow installation from your browser or file manager.

A debug APK is for testing, not a signed production release.

## Connect online AI

1. Create an account and API key at [OpenRouter API Keys](https://openrouter.ai/keys).
2. Open AnimeVerse AI and tap **AI KEY**.
3. Paste your key into the app and save it for the current session.
4. Keep the key private. Never send it to anyone or commit it to this repository.

The app requests `openrouter/free`, which routes to an available free model. Free inference has provider/model rate limits and availability can change; it is not unlimited. Do not add credits or enable paid fallback if you want to keep usage free. See [OpenRouter's free models information](https://openrouter.ai/collections/free-models) and [Free Models Router documentation](https://openrouter.ai/docs/guides/routing/routers/free).

## Current limitations

- The app stores recent chat history locally, but it does not yet create a semantic long-term memory summary. The AI receives only a limited recent slice of the conversation on each request.
- Character relationship points are a simple prototype counter, not yet a full relationship simulation.
- Added characters are included in the roleplay prompt, but there is not yet a full autonomous multi-character agent system or complex quest/branching-state engine.
- The API key is held in memory for the current app session and must be entered again after restarting.
- Live AI requires internet access and a working provider key. The APK can open without an API key, but AI replies will not work until one is entered.

## Product direction

The next development priorities are inspectable/editable long-term story memories, stronger relationship and consequence systems, multi-character scene participation, improved error handling, and original visual-novel artwork. Mature romance must remain limited to clearly adult, consenting characters; no sexual content involving minors or coercion.

## Build

The GitHub Actions workflow runs on pushes to `main` and can also be started manually from **Actions → Build AnimeVerse AI APK → Run workflow**. The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk` before it is uploaded as an artifact.

For a local build, use JDK 17, Android SDK platform 35, and the Gradle wrapper included in the repository:

```bash
./gradlew assembleDebug
```
