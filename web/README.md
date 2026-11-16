# AnimeVerse AI — free web experience

AnimeVerse AI is a mobile-friendly, player-led anime roleplay interface. It uses **no AI API and requires no API key**.

## How the ChatGPT handoff works
1. Type a character line or an action in parentheses in My Story, or describe a free-roam action in Living World.
2. Choose **Open ChatGPT**. AnimeVerse assembles your player profile, cast, current location, canon memory and recent conversation into a prompt.
3. ChatGPT opens in a separate tab. Send the prepared prompt there.
4. Copy ChatGPT's response back into AnimeVerse and choose **Save reply to my story**.

This is a manual handoff. A standalone website cannot directly access or control a user's signed-in ChatGPT conversation, and ChatGPT does not provide a free public browser API that can be embedded this way. No claim of automatic ChatGPT connectivity is made.

## Included
- Responsive mobile-first interface
- Local browser storage for profile, characters, story messages, locations, events and canon notes
- Free-roam action prompt builder with current world context
- Player agency rules: AI must not invent the player's dialogue, thoughts, feelings or actions
- Character discovery and user-created adult characters
- Group-cast context and evolving story continuity through saved prompts
- Art Studio prompt handoff to ChatGPT image creation when available in the user's ChatGPT account
- No API key, server, subscription or API billing required by this site

## Important limitations
- ChatGPT usage and image-generation availability depend on the user's own ChatGPT account and product limits.
- Replies must be copied back manually. The website cannot read ChatGPT's private session or automatically receive its answer.
- Data is stored in this browser on this device. Clearing browser storage removes it; this prototype has no account sync or cloud backup.
- This is not a fully autonomous always-running simulation. The AI advances the world when the player sends a prompt.
- Adult romance is restricted to clearly adult, consenting fictional characters; non-graphic intimacy only.

## Deploy
GitHub Pages deploys the contents of `web/` through `.github/workflows/deploy-web.yml`. Enable **Settings → Pages → Build and deployment → Source: GitHub Actions** in the repository if Pages has not been enabled. The workflow needs repository permission to create the Pages site.
