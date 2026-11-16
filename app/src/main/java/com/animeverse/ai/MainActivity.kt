package com.animeverse.ai

import android.os.Bundle
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
import org.json.JSONArray
import org.json.JSONObject

private val Ink = Color(0xFF100D1C)
private val Violet = Color(0xFF9B7BFF)
private val Panel = Color(0xFF211B32)
private val Muted = Color(0xFFB9B1C9)
private data class Hero(val name: String, val age: Int, val role: String, val bio: String)
private data class Line(val speaker: String, val body: String, val mine: Boolean)
private val starters = listOf(
    Hero("Aiko", 21, "Star Guardian", "A bright guardian of the Moonlit Academy."),
    Hero("Ren", 24, "Quiet Mage", "A reserved spellcaster with a hidden past."),
    Hero("Mika", 22, "Rival Hero", "A fearless adventurer who loves a challenge.")
)

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
    var draft by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var keyOpen by remember { mutableStateOf(false) }
    var aiBusy by remember { mutableStateOf(false) }
    val aiScope = rememberCoroutineScope()
    var createOpen by remember { mutableStateOf(false) }
    var memoryOpen by remember { mutableStateOf(false) }
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
            .putString("lines", writeLines(lines.takeLast(250))).putInt("chapter", chapter)
            .putInt("bonds", bonds).putBoolean("mature", mature).apply()
    }
    fun addLine(who: String, text: String, mine: Boolean) {
        lines = (lines + Line(who, text, mine)).takeLast(250)
        persist()
    }
    fun act(text: String) {
        if (apiKey.isBlank()) { keyOpen = true; return }
        val hero = heroes.firstOrNull { it.name == selected } ?: heroes.first()
        addLine("You", text, true)
        aiBusy = true
        aiScope.launch {
            try {
                val answer = AnimeVerseAi.reply(
                    apiKey = apiKey,
                    world = world,
                    activeCharacter = hero,
                    cast = heroes,
                    history = lines.takeLast(24),
                    matureRomance = mature
                )
                addLine(hero.name, answer, false)
                bonds += 1
                persist()
            } catch (e: Exception) {
                addLine("System", "AI connection failed: " + (e.message ?: "Please try again.") + " Check your internet and free-model API key.", false)
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
                TextButton(onClick = { memoryOpen = true }) { Text("MEMORY") }
                TextButton(onClick = { keyOpen = true }) { Text("AI KEY") }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(world, { world = it; persist() }, modifier = Modifier.weight(1f), label = { Text("Story world") }, singleLine = true)
                Spacer(Modifier.width(7.dp))
                Button(onClick = { createOpen = true }) { Text("+ Hero") }
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
                items(listOf("Start a quest", "Explore the academy", "Talk about romance", "Meet the whole cast")) { item ->
                    AssistChip(onClick = { act(item) }, label = { Text(item, fontSize = 10.sp) })
                }
            }
            Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(draft, { draft = it }, Modifier.weight(1f).background(Panel, RoundedCornerShape(24.dp)).padding(15.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                    decorationBox = { inner -> if (draft.isEmpty()) Text("Write what happens next…", color = Muted); inner() })
                Spacer(Modifier.width(7.dp))
                Button(onClick = { val value = draft.trim(); if (value.isNotEmpty() && !aiBusy) { act(value); if (apiKey.isNotBlank()) draft = "" } }, enabled = !aiBusy) { Text("Send") }
            }
            Text("Local save • no paid API required • prototype replies are scripted", color = Muted, fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 5.dp))
        }
    }

    if (keyOpen) AlertDialog(\n        onDismissRequest = { keyOpen = false },\n        title = { Text("Connect free online AI") },\n        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {\n            Text("Create a free OpenRouter account and an API key. Use free models only; do not add credits or enable paid usage. Free model availability and daily limits can change.", color = Muted)\n            OutlinedTextField(apiKey, { apiKey = it.trim() }, label = { Text("OpenRouter API key") }, singleLine = true)\n            Text("Your key is kept only in app memory for this session and is not saved to the story. Re-enter it after restarting the app.", color = Muted, fontSize = 11.sp)\n        } },\n        confirmButton = { TextButton(onClick = { keyOpen = false }) { Text("Save for this session") } },\n        dismissButton = { TextButton(onClick = { apiKey = ""; keyOpen = false }) { Text("Clear key") } }\n    )\n\n    if (createOpen) AlertDialog(
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
            Text("World: " + world + "\nChapter: " + chapter + "\nCast: " + heroes.joinToString { it.name + " (" + it.age + ")" } + "\nBond points: " + bonds + "\nSaved messages: " + lines.size, color = Color.White)
            Text("The cast and recent story history return when you reopen the app. This build uses scripted replies; live AI is not connected yet.", color = Muted, fontSize = 12.sp)
            TextButton(onClick = { chapter++; addLine("Narrator", "Chapter " + chapter + " begins. Your cast and earlier story remain.", false); memoryOpen = false }) { Text("Begin next chapter") }
            TextButton(onClick = { lines = listOf(Line("Narrator", "A fresh scene begins in " + world + ". Your cast remains: " + heroes.joinToString { it.name }, false)); persist() }) { Text("Clear conversation (keep cast)") }
        } },
        confirmButton = { TextButton(onClick = { memoryOpen = false }) { Text("Close") } }
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
