# AnimeVerse AI

AnimeVerse AI is an original Android anime-world roleplay project. Its goal is a living visual-novel experience: persistent characters, evolving relationships, branching stories, quests, and user-created worlds.

## Product direction

- **Living worlds:** story locations, quests, factions, and world events should persist between sessions.
- **Persistent memory:** preserve conversation history and structured facts such as character identities, relationships, promises, important events, and consequences. Users should be able to inspect, edit, and clear saved memories.
- **User-created cast:** characters added by the user become members of the active story world and can appear in scenes, interact with one another, and affect future events.
- **Branching narrative:** choices should change relationships, quests, world state, and endings.
- **Mature themes:** support adult romance and mature dramatic themes behind an 18+ age gate. Romantic/adult scenarios must involve clearly adult, consenting characters. No sexual content involving minors or coercion.
- **Free-first operation:** no subscriptions, paid unlocks, or required paid API keys. Prefer on-device/open-source models and local storage. Clearly disclose device requirements and any optional external hosting costs; never promise unlimited cloud inference at no cost.

## Current prototype (v0.1.0)

- Jetpack Compose Android app.
- Three selectable sample character labels and editable story-world field.
- Local chat interface with placeholder scripted replies.
- GitHub Actions workflow to build and upload a debug APK.

**Important:** the current APK is only a starter prototype. It does not yet provide live AI generation, durable story memory, a full character/world engine, accounts, or generated scene art/audio. Those are planned capabilities, not implemented features.

## Planned implementation phases

1. **Local-first foundation:** persistent world save, character profiles, conversation history, memory editor/export/delete, and story-state data models.
2. **Narrative engine:** scene/chapter progression, choice consequences, quests, relationship values, and continuity checks.
3. **AI runtime:** optional on-device open model integration, with a clear compatibility/device-performance screen. No secret keys embedded in the APK.
4. **Visual novel layer:** original character portraits, backgrounds, scene transitions, and optional user-controlled audio.
5. **Safety and privacy:** 18+ gate for mature romance, adult-only romantic character profiles, consent boundaries, and local-first privacy controls.

## Build

The GitHub Actions workflow runs on pushes to `main` and can also be started manually from **Actions → Build AnimeVerse AI APK → Run workflow**. Download the `AnimeVerse-AI-debug` artifact after the run succeeds.

For a local build, install JDK 17, Android SDK (platform 35), and Gradle 8.9, then run:

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.
