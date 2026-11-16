package com.animeverse.ai

import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tensai.llamakt.ChatMessage
import org.json.JSONArray
import org.json.JSONObject

private val Ink = Color(0xFF100D1C)
private val Violet = Color(0xFF9B7BFF)
private val Panel = Color(0xFF211B32)
private val Muted = Color(0xFFB9B1C9)
private data class Hero(val name: String, val age: Int, val role: String, val bio: String)
private data class Line(val speaker: String, val body: String, val mine: Boolean)
private data class WorldPlace(val name: String, val region: String, val description: String)
private data class WorldReply(val text: String, val location: String?, val people: List<Hero>)

private fun parseWorldReply(raw: String): WorldReply {
    val locationPattern = Regex("""\[\[WORLD_LOCATION:(.{1,80}?)\]\]""")
    val peoplePattern = Regex("""\[\[WORLD_NPC:([^|]{1,40})\|(\d{1,3})\|([^|]{1,60})\|(.{1,240}?)\]\]""")
    val location = locationPattern.find(raw)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    val people = peoplePattern.findAll(raw).mapNotNull { match ->
        val age = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
        if (age !in 18..120) return@mapNotNull null
        Hero(match.groupValues[1].trim(), age, match.groupValues[3].trim(), match.groupValues[4].trim())
    }.filter { it.name.isNotBlank() }.distinctBy { it.name.lowercase() }.toList()
    val cleaned = raw.replace(locationPattern, "").replace(peoplePattern, "").trim()
    return WorldReply(cleaned.ifBlank { "The world stirs around you, but no clear response arrived. Try describing your action again." }, location, people)
}
private val worldPlaces = listOf(
    WorldPlace("Moonlit Academy", "Lunaris City", "A vast academy with dormitories, classrooms, gardens, a library and a busy central courtyard."),
    WorldPlace("Silverleaf Market", "Lunaris City", "A lively market of food stalls, small shops, street performers and merchants from distant regions."),
    WorldPlace("Lantern Street", "Lunaris City", "A warm evening district filled with cafes, apartments, music venues and narrow side streets."),
    WorldPlace("Royal Archive", "Lunaris City", "An old public archive containing maps, records, forgotten letters and carefully guarded secrets."),
    WorldPlace("Whispering Forest", "Outer Lands", "A deep woodland with trails, streams, ruins, wildlife and occasional travelers."),
    WorldPlace("Harbor of Glass", "Coastal Reach", "A crowded harbor where fishing boats, merchant ships and travelers arrive throughout the day.")
)
private val roamingPeople = listOf(
    Hero("Emi", 22, "Apprentice Healer", "Patient and observant, she volunteers at the academy clinic and worries about everyone except herself."),
    Hero("Daichi", 26, "Market Courier", "Quick-witted, restless and well-connected; he knows the city's shortcuts and local gossip."),
    Hero("Natsumi", 23, "Street Violinist", "Independent and quietly funny, she plays at Lantern Street and dislikes being treated like a celebrity."),
    Hero("Toma", 28, "Archive Keeper", "A meticulous historian with a dry sense of humour and a fascination with missing records."),
    Hero("Ayame", 24, "Forest Guide", "Practical, brave and protective of the forest; she trusts actions more than promises."),
    Hero("Jun", 21, "Harbor Deckhand", "Friendly and adventurous, he dreams of sailing beyond the known coastal routes."),
    Hero("Kaede", 25, "Flower Shop Owner", "Gentle but outspoken, she remembers customers and notices changes in the neighbourhood."),
    Hero("Shin", 27, "Traveling Chef", "Sociable, competitive and always searching for unusual ingredients and recipes."),
    Hero("Mei", 22, "Student Researcher", "Curious and determined, she asks difficult questions and follows clues others ignore."),
    Hero("Haru", 29, "Retired Adventurer", "Calm and guarded, he gives thoughtful advice but avoids discussing his past.")
)
private fun discoverFilterPlaceholder(hero: Hero, favorites: Set<String>) = true
private val starters = listOf(
    Hero("Aiko", 21, "Star Guardian", "A bright guardian of the Moonlit Academy."),
    Hero("Ren", 24, "Quiet Mage", "A reserved spellcaster with a hidden past."),
    Hero("Mika", 22, "Rival Hero", "A fearless adventurer who loves a challenge.")
)


private object AnimeVerseAi {
    suspend fun reply(
        apiKey: String,
        world: String,
        activeCharacter: Hero,
        cast: List<Hero>,
        history: List<Line>,
        matureRomance: Boolean,
        storyMemory: String,
        worldBuild: Boolean,
        livingWorld: Boolean = false,
        localRuntime: LocalAiRuntime? = null
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("HTTP-Referer", "https://github.com/athukoralaprabath8-design/AnimeVerse-AI")
            setRequestProperty("X-Title", "AnimeVerse AI")
        }
        try {
            val castInfo = cast.joinToString("\\n") { "${it.name}, age ${it.age}, role ${it.role}, personality/backstory: ${it.bio}" }
            val livingWorldInstructions = if (livingWorld) " ACTIVE MODE: FREE-ROAMING LIVING WORLD. Treat text in parentheses as the player's attempted action and resolve it naturally in the scene. This is an open-ended simulated world, not a menu or a fixed list of places/NPCs. Invent coherent locations, businesses, streets, homes, public venues and inhabitants when the player's action takes them somewhere new. Create believable adult NPCs with distinct names, ages (18+), jobs, personalities, motives and immediate circumstances; let them approach, work, talk to each other and respond naturally. If the player enters a venue, describe the actual atmosphere and who is present; let staff or visitors initiate plausible conversation without forcing intimacy or assuming consent. Remember new people, locations, promises, events and consequences as canon. Do not require the player to select a predefined character or location. At the very end append exactly one hidden state marker [[WORLD_LOCATION:Current location name]]. For every newly introduced recurring named NPC, append a marker [[WORLD_NPC:Name|Age|Role|Short personality and identifying details]]; age must be 18 or older. These markers are for app memory and will be removed before display. Do not include any other machine markers." else ""
            val system = "You are the roleplay engine and living world architect for AnimeVerse AI. Write immersive, emotionally believable anime roleplay. Persistent story memory (treat as established canon unless the user changes it): $storyMemory. World: $world. Active character: ${activeCharacter.name}, age ${activeCharacter.age}, role ${activeCharacter.role}, personality: ${activeCharacter.bio}. Full cast (all may participate naturally):\\n$castInfo\\nStay in character, remember established details in the conversation, show nuanced feelings and agency, and let user choices have consequences. The user is the only author of their own character and the driver of the story. Never write the user's dialogue, thoughts, feelings, decisions, intentions, movements, reactions, or actions. Never assume the user agreed, followed, smiled, touched someone, entered a place, accepted a quest, or did anything they did not explicitly say. Never narrate what the user says, feels, or decides. Stop whenever the user must choose or act. Respond directly and specifically to the user's latest words and actions; do not replace them with a prewritten plot or force a direction. Characters are independent fictional people with stable, distinct voices, motives, boundaries, tastes, flaws, habits, and personal goals. Make them feel human through natural conversational rhythm, contractions, subtext, humor, hesitation, curiosity, emotional nuance, changing moods, disagreement, vulnerability, and believable consequences. Let them remember shared events and relationship history; relationships change gradually through what actually happens, not automatic point farming. Characters may initiate their own dialogue or reasonable actions, but never take control of the user or skip an important user decision. Do not make everyone instantly agree, admire, love, or obey the user. Do not make every character speak in every reply; let the relevant person respond, using clear names when multiple characters naturally speak. Avoid repetitive exposition, generic narration, scripted menus, and ending every message with a question. When worldBuild mode is active, act as the World Architect: create an original, coherent setting bible with a compelling premise, geography and important locations, cultures, factions, everyday life, history, mysteries, rules of magic or technology, conflicts, and several memorable supporting NPCs with distinct motives. Make details specific and internally consistent, leave room for the user to shape canon, and never dictate the user character. Format the result with clear headings and practical details. The app will save this generated world bible as persistent canon. WorldBuild mode: $worldBuild. Prefer natural in-character dialogue with concise sensory details only when useful. For normal roleplay, write 1-3 vivid paragraphs and end naturally without forcing a question every time. In worldBuild mode, use the requested world-bible headings and enough detail to make the setting usable. Mature romance setting: ${if (matureRomance) "enabled only for clearly adult consenting characters; allow attraction, sensual romantic tension, flirting, kissing and non-graphic intimacy, but do not generate explicit sexual descriptions or pornographic detail" else "disabled; keep romance non-explicit and age-appropriate"}. Never sexualize minors. Treat this as fictional roleplay. $livingWorldInstructions"
            if (localRuntime != null) {
                val localMessages = history.takeLast(22).map { line -> ChatMessage(if (line.mine) "user" else "assistant", if (line.mine) line.body else "${line.speaker}: ${line.body}") }
                return@withContext localRuntime.generate(system, localMessages)
            }
            val messages = JSONArray().put(JSONObject().put("role", "system").put("content", system))
            history.takeLast(22).forEach { line ->
                messages.put(JSONObject().put("role", if (line.mine) "user" else "assistant").put("content", if (line.mine) line.body else "${line.speaker}: ${line.body}"))
            }
            val payload = JSONObject().put("model", "openrouter/free").put("messages", messages)
                .put("provider", JSONObject().put("allow_fallbacks", true))
                .put("temperature", 0.85).put("max_tokens", 600)
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val root = runCatching { JSONObject(raw) }.getOrElse {
                throw IllegalStateException("The AI service sent an unreadable response (HTTP $code). Please try again.")
            }
            val providerError = root.optJSONObject("error")?.optString("message").orEmpty()
            if (code !in 200..299 || providerError.isNotBlank()) {
                throw IllegalStateException("AI service error ($code): " +
                    providerError.ifBlank { "No free model is available right now." }.take(220))
            }
            val choices = root.optJSONArray("choices")
                ?: throw IllegalStateException("The free AI provider returned no reply. Please try again.")
            if (choices.length() == 0) throw IllegalStateException("The free AI provider returned an empty result. Please try again.")
            val choice = choices.optJSONObject(0)
                ?: throw IllegalStateException("The AI response was incomplete. Please try again.")
            val message = choice.optJSONObject("message")
                ?: throw IllegalStateException("The AI provider did not include a message. Please try again.")
            val rawContent = message.opt("content")
            val content = when (rawContent) {
                is String -> rawContent.trim()
                is JSONArray -> (0 until rawContent.length()).mapNotNull { index ->
                    rawContent.optJSONObject(index)?.optString("text")?.takeIf { it.isNotBlank() && it != "null" }
                }.joinToString("\n").trim()
                else -> ""
            }
            if (content.isBlank() || content.equals("null", ignoreCase = true)) {
                val reason = choice.optString("finish_reason")
                throw IllegalStateException("The AI provider returned no text" +
                    (if (reason.isNotBlank() && reason != "null") " (reason: $reason)" else "") +
                    ". Free models can be busy; please try again in a few seconds.")
            }
            content
        } finally { connection.disconnect() }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AnimeVerseApp() }
    }
}

@Composable
private fun AnimeVerseApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("animeverse_world_v1", 0) }
    var world by remember { mutableStateOf(prefs.getString("world", "The Moonlit Academy") ?: "The Moonlit Academy") }
    var heroes by remember { mutableStateOf(readHeroes(prefs.getString("heroes", null))) }
    var lines by remember { mutableStateOf(readLines(prefs.getString("lines", null)).ifEmpty { listOf(Line("Narrator", "Moonlight spills across the academy courtyard. Your adventure begins here.", false)) }) }
    var selected by remember { mutableStateOf("Aiko") }
    var showDiscover by remember { mutableStateOf(false) }
    var showImages by remember { mutableStateOf(false) }
    var imagePrompt by remember { mutableStateOf("") }
    var imageApiKey by remember { mutableStateOf("") }
    var imageKeyOpen by remember { mutableStateOf(false) }
    var imageBusy by remember { mutableStateOf(false) }
    var generatedImage by remember { mutableStateOf<Bitmap?>(null) }
    var imageStatus by remember { mutableStateOf("Create original anime-style character art and world scenes.") }
    var showWorld by remember { mutableStateOf(false) }
    var playerName by remember { mutableStateOf(prefs.getString("player_name", "") ?: "") }
    var playerAge by remember { mutableStateOf(prefs.getString("player_age", "21") ?: "21") }
    var playerBackground by remember { mutableStateOf(prefs.getString("player_background", "") ?: "") }
    var currentLocation by remember { mutableStateOf(prefs.getString("current_location", "Moonlit Academy") ?: "Moonlit Academy") }
    var worldDraft by remember { mutableStateOf("") }
    var discoveredPeople by remember { mutableStateOf(prefs.getStringSet("discovered_people", emptySet())?.toSet() ?: emptySet()) }
    var discoverQuery by remember { mutableStateOf("") }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorite_characters", emptySet())?.toSet() ?: emptySet()) }
    var draft by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    val localRuntime = remember { LocalAiRuntime(context) }
    var aiMode by remember { mutableStateOf("Auto") }
    var localReady by remember { mutableStateOf(localRuntime.isLoaded()) }
    var modelDownloaded by remember { mutableStateOf(localRuntime.isDownloaded()) }
    var modelBusy by remember { mutableStateOf(false) }
    var modelProgress by remember { mutableFloatStateOf(0f) }
    var localStatus by remember { mutableStateOf(if (localRuntime.isDownloaded()) "Model downloaded; load it to use offline AI." else "Offline model not downloaded.") }
    var keyOpen by remember { mutableStateOf(false) }
    var aiBusy by remember { mutableStateOf(false) }
    val aiScope = rememberCoroutineScope()
    DisposableEffect(localRuntime) { onDispose { localRuntime.close() } }
    var createOpen by remember { mutableStateOf(false) }
    var memoryOpen by remember { mutableStateOf(false) }
    var storyMemory by remember { mutableStateOf(prefs.getString("story_memory", "") ?: "") }
    var memoryDraft by remember { mutableStateOf("") }
    var ageOpen by remember { mutableStateOf(!prefs.getBoolean("age_confirmed", false)) }
    var mature by remember { mutableStateOf(prefs.getBoolean("mature", false)) }
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("21") }
    var role by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var chapter by remember { mutableIntStateOf(prefs.getInt("chapter", 1)) }
    var bonds by remember { mutableIntStateOf(prefs.getInt("bonds", 0)) }

    fun persist() {
        prefs.edit().putString("world", world).putString("heroes", writeHeroes(heroes))
            .putString("lines", writeLines(lines.takeLast(250))).putString("story_memory", storyMemory).putInt("chapter", chapter)
            .putInt("bonds", bonds).putBoolean("mature", mature).putStringSet("favorite_characters", favorites).putString("player_name", playerName).putString("player_age", playerAge).putString("player_background", playerBackground).putString("current_location", currentLocation).putStringSet("discovered_people", discoveredPeople).apply()
    }
    fun addLine(who: String, text: String, mine: Boolean) {
        lines = (lines + Line(who, text, mine)).takeLast(250)
        persist()
    }
    fun recordAiAnswer(answer: String, worldBuild: Boolean, livingWorld: Boolean) {
        if (livingWorld) {
            val parsed = parseWorldReply(answer)
            if (!parsed.location.isNullOrBlank()) currentLocation = parsed.location
            if (parsed.people.isNotEmpty()) {
                heroes = (heroes + parsed.people).distinctBy { it.name.lowercase() }
                discoveredPeople = discoveredPeople + parsed.people.map { it.name }
            }
            addLine("LIVING WORLD", parsed.text, false)
            storyMemory = (storyMemory + "\n\nWORLD EVENT at " + currentLocation + ": " + parsed.text).takeLast(12000)
        } else {
            addLine(if (worldBuild) "WORLD ARCHITECT" else (heroes.firstOrNull { it.name == selected }?.name ?: "Narrator"), answer, false)
            if (worldBuild) storyMemory = (storyMemory + "\n\nWORLD BIBLE (AI-GENERATED CANON):\n" + answer).takeLast(12000)
        }
        bonds += 1
        persist()
    }

    fun act(text: String, worldBuild: Boolean = false, livingWorld: Boolean = false) {
        val useLocal = aiMode == "Local" || (aiMode == "Auto" && localRuntime.isLoaded())
        if (aiMode == "Local" && !localRuntime.isLoaded()) { localStatus = "Download and load the local model first."; return }
        if (!useLocal && apiKey.isBlank()) { keyOpen = true; return }
        val hero = heroes.firstOrNull { it.name == selected } ?: heroes.first()
        addLine("You", text, true)
        aiBusy = true
        aiScope.launch {
            try {
                val answer = AnimeVerseAi.reply(
                    apiKey = apiKey,
                    world = world + ". Current location: " + currentLocation + ". Player character: " + (playerName.ifBlank { "unnamed player character" }) + ", age " + playerAge + ", background: " + playerBackground + ". The user alone controls this character.",
                    activeCharacter = hero,
                    cast = heroes,
                    history = lines.takeLast(24),
                    matureRomance = mature,
                    storyMemory = storyMemory,
                    worldBuild = worldBuild,
                    livingWorld = livingWorld,
                    localRuntime = if (useLocal) localRuntime else null
                )
                recordAiAnswer(answer, worldBuild, livingWorld)
            } catch (e: Exception) {
                if (useLocal && aiMode == "Auto" && apiKey.isNotBlank()) {
                    try {
                        val fallback = AnimeVerseAi.reply(apiKey = apiKey, world = world + ". Current location: " + currentLocation + ". Player character: " + (playerName.ifBlank { "unnamed player character" }) + ", age " + playerAge + ", background: " + playerBackground + ". The user alone controls this character.", activeCharacter = hero, cast = heroes, history = lines.takeLast(24), matureRomance = mature, storyMemory = storyMemory, worldBuild = worldBuild, livingWorld = livingWorld, localRuntime = null)
                        recordAiAnswer(fallback, worldBuild, livingWorld)
                    } catch (fallbackError: Exception) {
                        addLine("System", "Local AI failed (" + (e.message ?: "unknown error") + "); online fallback failed (" + (fallbackError.message ?: "unknown error") + ").", false)
                    }
                } else {
                    addLine("System", "AI connection failed: " + (e.message ?: "Please try again.") + " Check your internet and free-model API key.", false)
                }
            } finally { aiBusy = false }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Violet, background = Ink, surface = Panel)) {
        Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().background(Panel).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ANIMEVERSE AI", color = Color(0xFFC8B7FF), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text("A world that remembers.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                }
                TextButton(onClick = { memoryDraft = storyMemory; memoryOpen = true }) { Text("MEMORY") }
                TextButton(onClick = { keyOpen = true }) { Text("AI KEY") }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = !showWorld && !showDiscover && !showImages, onClick = { showWorld = false; showDiscover = false; showImages = false }, label = { Text("MY STORY") })
                FilterChip(selected = showWorld, onClick = { showWorld = true; showDiscover = false; showImages = false }, label = { Text("LIVING WORLD") })
                FilterChip(selected = showDiscover, onClick = { showDiscover = true; showWorld = false; showImages = false }, label = { Text("CHARACTERS") })
                FilterChip(selected = showImages, onClick = { showImages = true; showWorld = false; showDiscover = false }, label = { Text("ART") })
            }
            if (showWorld) {
                val place = worldPlaces.firstOrNull { it.name == currentLocation } ?: WorldPlace(currentLocation, "Discovered location", "A place discovered through your actions in the living world.")
                Column(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("YOUR LIFE IN THE WORLD", color = Color(0xFFC8B7FF), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Create your own identity. The world is yours to explore.", color = Muted, fontSize = 12.sp)
                    OutlinedTextField(playerName, { playerName = it; persist() }, modifier = Modifier.fillMaxWidth(), label = { Text("Your character name") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(playerAge, { playerAge = it.filter(Char::isDigit).take(3); persist() }, modifier = Modifier.weight(0.35f), label = { Text("Age") }, singleLine = true)
                        OutlinedTextField(playerBackground, { playerBackground = it; persist() }, modifier = Modifier.weight(0.65f), label = { Text("Background / identity") }, singleLine = true)
                    }
                    Text("CURRENT LOCATION", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(place.name + " · " + place.region, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(place.description, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                    Text("TRAVEL TO", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(worldPlaces) { destination ->
                            AssistChip(onClick = {
                                currentLocation = destination.name
                                persist()
                                addLine("World", "You arrive at " + destination.name + ". " + destination.description, false)
                            }, label = { Text(destination.name, fontSize = 10.sp) })
                        }
                    }
                    Text("WORLD SCENE & RECENT EVENTS", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 6.dp)) {
                        items(lines.takeLast(16)) { line ->
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = if (line.mine) Alignment.End else Alignment.Start) {
                                Text(if (line.mine) "YOU" else line.speaker.uppercase(), color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Box(Modifier.fillMaxWidth(0.94f).background(if (line.mine) Color(0xFF49357A) else Panel, RoundedCornerShape(12.dp)).padding(10.dp)) {
                                    Text(line.body, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                                }
                            }
                        }
                        item { Text("PEOPLE YOU HAVE MET · " + discoveredPeople.size, color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        items(discoveredPeople.toList()) { personName ->
                            val person = heroes.firstOrNull { it.name.equals(personName, true) } ?: roamingPeople.firstOrNull { it.name.equals(personName, true) }
                            if (person != null) {
                                Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(12.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(person.name + " · " + person.age, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(person.role, color = Violet, fontSize = 11.sp)
                                    }
                                    TextButton(onClick = { if (heroes.none { it.name.equals(person.name, true) }) heroes = heroes + person; selected = person.name; showWorld = false; addLine("Narrator", person.name + " is ready to talk with you.", false); persist() }) { Text("TALK") }
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = worldDraft,
                        onValueChange = { worldDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Describe what you do in the world") },
                        placeholder = { Text("Example: (I enter the inn and look for someone to talk to.)") },
                        minLines = 2,
                        maxLines = 4
                    )
                    Button(
                        onClick = {
                            val action = worldDraft.trim()
                            if (action.isNotEmpty() && !aiBusy) {
                                val canSend = (aiMode == "Local" && localRuntime.isLoaded()) ||
                                    (aiMode == "Online" && apiKey.isNotBlank()) ||
                                    (aiMode == "Auto" && (localRuntime.isLoaded() || apiKey.isNotBlank()))
                                act(action, livingWorld = true)
                                if (canSend) worldDraft = ""
                            }
                        },
                        enabled = !aiBusy && worldDraft.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (aiBusy) "THE WORLD IS RESPONDING…" else "DO ACTION") }
                }
            } else if (showDiscover) {
                val library = (starters + listOf(
                    Hero("Yuna", 23, "Wandering Swordmaster", "Calm, observant and quietly competitive. She protects travelers but rarely speaks about her past."),
                    Hero("Kaito", 25, "Skyship Captain", "A charming risk-taker with a strict personal code and a crew he considers family."),
                    Hero("Hana", 21, "Cafe Musician", "Warm but independent, writes songs about people she meets."),
                    Hero("Riku", 24, "Detective", "Methodical, skeptical and unexpectedly funny. He notices details others miss."),
                    Hero("Sora", 22, "Arcane Researcher", "Curious, stubborn and brilliant, still learning when to ask for help.")
                ) + heroes).distinctBy { it.name.lowercase() }
                val filtered = library.filter { h ->
                    (discoverQuery.isBlank() || listOf(h.name, h.role, h.bio).any { it.contains(discoverQuery, true) }) &&
                    (discoverFilterPlaceholder(h, favorites))
                }
                Column(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("CHARACTER DISCOVERY", color = Color(0xFFC8B7FF), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Find a new companion, rival, friend or story.", color = Muted, fontSize = 12.sp)
                    OutlinedTextField(discoverQuery, { discoverQuery = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search names, roles and personalities") }, singleLine = true)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 14.dp)) {
                        items(filtered) { hero ->
                            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(15.dp)).padding(13.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(hero.name + " · " + hero.age, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                        Text(hero.role, color = Violet, fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { favorites = if (hero.name in favorites) favorites - hero.name else favorites + hero.name; persist() }) {
                                        Text(if (hero.name in favorites) "♥ SAVED" else "♡ SAVE")
                                    }
                                }
                                Text(hero.bio, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = {
                                    if (heroes.none { it.name.equals(hero.name, true) }) heroes = heroes + hero
                                    selected = hero.name
                                    showDiscover = false
                                    addLine("Narrator", hero.name + " joins your cast in " + world + ".", false)
                                    persist()
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (heroes.any { it.name.equals(hero.name, true) }) "OPEN IN MY STORY" else "ADD TO MY STORY")
                                }
                            }
                        }
                    }
                }
            } else if (showImages) {
                Column(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ANIME ART STUDIO", color = Color(0xFFC8B7FF), fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Generate original portraits, outfits, dramatic moments and locations for your story.", color = Muted, fontSize = 12.sp)
                    OutlinedTextField(
                        value = imagePrompt,
                        onValueChange = { imagePrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Describe the image") },
                        placeholder = { Text("A silver-haired adult mage in a moonlit academy garden...") },
                        minLines = 3,
                        maxLines = 5
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedButton(onClick = { imagePrompt = "Original adult anime character portrait, elegant fantasy outfit, soft cinematic lighting, detailed eyes" }, modifier = Modifier.weight(1f)) { Text("CHARACTER", fontSize = 10.sp) }
                        OutlinedButton(onClick = { imagePrompt = "Beautiful anime fantasy city at twilight, glowing lanterns, detailed architecture, cinematic atmosphere" }, modifier = Modifier.weight(1f)) { Text("LOCATION", fontSize = 10.sp) }
                    }
                    Button(
                        onClick = {
                            if (imageApiKey.isBlank()) imageKeyOpen = true
                            else if (imagePrompt.isNotBlank() && !imageBusy) {
                                imageBusy = true
                                imageStatus = "Creating your anime artwork…"
                                aiScope.launch {
                                    try {
                                        generatedImage = AnimeVerseAi.generateAnimeImage(imageApiKey, imagePrompt.trim())
                                        imageStatus = "Artwork generated. Change the description to create another image."
                                    } catch (e: Exception) {
                                        imageStatus = e.message ?: "Image generation failed. Please try again."
                                    } finally { imageBusy = false }
                                }
                            }
                        },
                        enabled = !imageBusy && imagePrompt.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (imageBusy) "GENERATING ART…" else "GENERATE ANIME IMAGE") }
                    Text(imageStatus, color = Muted, fontSize = 11.sp)
                    generatedImage?.let { art ->
                        Image(
                            bitmap = art.asImageBitmap(),
                            contentDescription = "Generated AnimeVerse AI artwork",
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentScale = ContentScale.Fit
                        )
                    } ?: Spacer(Modifier.weight(1f))
                    TextButton(onClick = { imageKeyOpen = true }) { Text(if (imageApiKey.isBlank()) "CONNECT IMAGE GENERATION" else "CHANGE IMAGE API KEY") }
                    Text("Image generation uses a separate provider key. Usage limits and any provider charges depend on that provider.", color = Muted, fontSize = 9.sp)
                }
            } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(world, { world = it; persist() }, modifier = Modifier.weight(1f), label = { Text("Story world") }, singleLine = true)
                Spacer(Modifier.width(7.dp))
                Button(onClick = { createOpen = true }) { Text("+ Hero") }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Local", "Online", "Auto").forEach { mode -> FilterChip(selected = aiMode == mode, onClick = { aiMode = mode }, label = { Text(mode) }) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (localReady) "ON-DEVICE AI READY" else if (modelDownloaded) "LOCAL MODEL DOWNLOADED" else "LOCAL AI NOT INSTALLED", color = if (localReady) Color(0xFF8FE0B0) else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(localStatus, color = Muted, fontSize = 10.sp)
                    if (modelBusy) LinearProgressIndicator(progress = { modelProgress }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                }
                TextButton(enabled = !modelBusy, onClick = {
                    aiScope.launch {
                        modelBusy = true
                        try {
                            if (!localRuntime.isDownloaded()) { localStatus = "Downloading model…"; localRuntime.download { p -> modelProgress = p } }
                            modelDownloaded = localRuntime.isDownloaded()
                            localStatus = "Loading model into memory…"
                            localRuntime.load { p -> modelProgress = p }
                            localReady = localRuntime.isLoaded()
                            localStatus = "Offline AI is ready. You can switch to Local mode."
                        } catch (e: Exception) { localStatus = "Local AI setup failed: " + (e.message ?: "unknown error") }
                        finally { modelBusy = false }
                    }
                }) { Text(if (modelBusy) "PLEASE WAIT" else if (modelDownloaded) "LOAD MODEL" else "DOWNLOAD (~400 MB)") }
            }
            Text("YOUR CAST  •  " + heroes.size + " CHARACTERS", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(heroes) { h -> FilterChip(selected == h.name, { selected = h.name }, label = { Text(h.name + " · " + h.age) }) }
            }
            val active = heroes.firstOrNull { it.name == selected }
            if (active != null) Text(active.role + "  •  " + active.bio, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 13.dp), verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
                items(lines) { line ->
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (line.mine) Alignment.End else Alignment.Start) {
                        Text(if (line.mine) "YOU" else line.speaker.uppercase(), color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Box(Modifier.widthIn(max = 340.dp).background(if (line.mine) Color(0xFF49357A) else Panel, RoundedCornerShape(15.dp)).padding(13.dp)) {
                            Text(line.body, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }
            if (aiBusy) Text("AI IS THINKING…", color = Violet, fontSize = 10.sp, modifier = Modifier.padding(start = 16.dp))
            Text("QUICK CHOICES", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                items(listOf("Build my world", "Start a quest", "Explore the academy", "Talk about romance", "Meet the whole cast")) { item ->
                    AssistChip(onClick = { if (item == "Build my world") act("Create a rich, original world bible for our setting. Invent its locations, cultures, factions, history, mysteries, rules, conflicts, and supporting characters. Keep it coherent and leave my character entirely under my control.", true) else act(item) }, label = { Text(item, fontSize = 10.sp) })
                }
            }
            Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(draft, { draft = it }, Modifier.weight(1f).background(Panel, RoundedCornerShape(24.dp)).padding(15.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                    decorationBox = { inner -> if (draft.isEmpty()) Text("Write what happens next…", color = Muted); inner() })
                Spacer(Modifier.width(7.dp))
                Button(onClick = { val value = draft.trim(); if (value.isNotEmpty() && !aiBusy) { act(value); if (apiKey.isNotBlank()) draft = "" } }, enabled = !aiBusy) { Text("Send") }
            }
            Text("Local story save • online AI • free-model availability varies", color = Muted, fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 5.dp))
            }
        }
    }

    if (keyOpen) AlertDialog(
        onDismissRequest = { keyOpen = false },
        title = { Text("Connect free online AI") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Create a free OpenRouter account and an API key. Use free models only; do not add credits or enable paid usage. Free model availability and daily limits can change.", color = Muted)
                OutlinedTextField(apiKey, { apiKey = it.trim() }, label = { Text("OpenRouter API key") }, singleLine = true)
                Text("Your key is kept only in app memory for this session and is not saved to the story. Re-enter it after restarting the app.", color = Muted, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(onClick = { keyOpen = false }) { Text("Save for this session") } },
        dismissButton = { TextButton(onClick = { apiKey = ""; keyOpen = false }) { Text("Clear key") } }
    )

    if (imageKeyOpen) AlertDialog(
        onDismissRequest = { imageKeyOpen = false },
        title = { Text("Connect anime image generation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AnimeVerse AI uses Pollinations image generation. Create a restricted secret API key at enter.pollinations.ai with image-only access and a small usage budget, then paste it here. The key stays in app memory for this session and is not saved to your story.", color = Muted)
                OutlinedTextField(imageApiKey, { imageApiKey = it.trim() }, label = { Text("Pollinations secret API key") }, singleLine = true)
                Text("Never share your secret key or put it in public GitHub files. Provider limits and prices can change.", color = Muted, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(onClick = { imageKeyOpen = false }) { Text("Use this key") } },
        dismissButton = { TextButton(onClick = { imageApiKey = ""; imageKeyOpen = false }) { Text("Clear key") } }
    )

    if (createOpen) AlertDialog(
        onDismissRequest = { createOpen = false },
        title = { Text("Add a character to your world") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
            OutlinedTextField(age, { age = it.filter(Char::isDigit).take(3) }, label = { Text("Age (18+ for mature romance)") }, singleLine = true)
            OutlinedTextField(role, { role = it }, label = { Text("Role / class") }, singleLine = true)
            OutlinedTextField(bio, { bio = it }, label = { Text("Personality / backstory") }, minLines = 2)
        } },
        confirmButton = { TextButton(onClick = {
            val years = age.toIntOrNull() ?: 0
            if (name.isNotBlank() && years in 1..120 && heroes.none { it.name.equals(name.trim(), true) }) {
                val newHero = Hero(name.trim(), years, role.ifBlank { "Adventurer" }, bio.ifBlank { "A new face in this world." })
                heroes = heroes + newHero
                selected = newHero.name
                addLine("Narrator", newHero.name + " has joined " + world + ". Your new cast: " + heroes.joinToString { it.name }, false)
                name = ""; age = "21"; role = ""; bio = ""; createOpen = false; persist()
            }
        }) { Text("Add to story") } },
        dismissButton = { TextButton(onClick = { createOpen = false }) { Text("Cancel") } }
    )

    if (memoryOpen) AlertDialog(
        onDismissRequest = { memoryOpen = false },
        title = { Text("Persistent world memory") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Saved on this device", color = Violet, fontWeight = FontWeight.Bold)
            Text("World: " + world + "\\nChapter: " + chapter + "\\nCast: " + heroes.joinToString { it.name + " (" + it.age + ")" } + "\\nBond points: " + bonds + "\\nSaved messages: " + lines.size, color = Color.White)
            Text("Story notes / important events", color = Color.White, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(memoryDraft, { memoryDraft = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 7, label = { Text("What should the AI remember?") }, placeholder = { Text("Example: Aiko promised to protect the academy. Ren knows the secret passage. The group trusts Mika...") })
            Text("These notes are saved locally and included in the AI prompt every time you chat. Edit them whenever the story changes.", color = Muted, fontSize = 12.sp)
            TextButton(onClick = { chapter++; addLine("Narrator", "Chapter " + chapter + " begins. Your cast and earlier story remain.", false); memoryOpen = false }) { Text("Begin next chapter") }
            TextButton(onClick = { lines = listOf(Line("Narrator", "A fresh scene begins in " + world + ". Your cast remains: " + heroes.joinToString { it.name }, false)); persist() }) { Text("Clear conversation (keep cast)") }
        } },
        confirmButton = { TextButton(onClick = { storyMemory = memoryDraft.trim(); persist(); memoryOpen = false }) { Text("Save memory") } },
        dismissButton = { TextButton(onClick = { memoryDraft = storyMemory; memoryOpen = false }) { Text("Cancel") } }
    )

    if (ageOpen) AlertDialog(
        onDismissRequest = { ageOpen = false; prefs.edit().putBoolean("age_confirmed", true).apply() },
        title = { Text("Content settings") },
        text = { Column {
            Text("Mature romance themes are limited to clearly adult, consenting characters.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(mature, { mature = it })
                Text("I am 18+ and want mature romance themes")
            }
        } },
        confirmButton = { TextButton(onClick = { prefs.edit().putBoolean("age_confirmed", true).putBoolean("mature", mature).apply(); ageOpen = false }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = { mature = false; prefs.edit().putBoolean("age_confirmed", true).putBoolean("mature", false).apply(); ageOpen = false }) { Text("General themes") } }
    )
}

private fun readHeroes(raw: String?): List<Hero> {
    if (raw.isNullOrBlank()) return starters
    return try { val a = JSONArray(raw); (0 until a.length()).map { val o = a.getJSONObject(it); Hero(o.optString("name"), o.optInt("age", 18), o.optString("role"), o.optString("bio")) }.ifEmpty { starters } } catch (_: Exception) { starters }
}
private fun writeHeroes(items: List<Hero>): String {
    val a = JSONArray()
    items.forEach { a.put(JSONObject().put("name", it.name).put("age", it.age).put("role", it.role).put("bio", it.bio)) }
    return a.toString()
}
private fun readLines(raw: String?): List<Line> {
    if (raw.isNullOrBlank()) return emptyList()
    return try { val a = JSONArray(raw); (0 until a.length()).map { val o = a.getJSONObject(it); Line(o.optString("speaker"), o.optString("body"), o.optBoolean("mine")) } } catch (_: Exception) { emptyList() }
}
private fun writeLines(items: List<Line>): String {
    val a = JSONArray()
    items.forEach { a.put(JSONObject().put("speaker", it.speaker).put("body", it.body).put("mine", it.mine)) }
    return a.toString()
}
