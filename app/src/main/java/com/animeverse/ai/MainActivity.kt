package com.animeverse.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF171326)
private val Violet = Color(0xFF8B5CF6)
private val SurfaceDark = Color(0xFF251F38)
private data class ChatLine(val speaker: String, val text: String, val mine: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AnimeVerseApp() }
    }
}

@Composable
fun AnimeVerseApp() {
    var character by remember { mutableStateOf("Aiko • Star Guardian") }
    var draft by remember { mutableStateOf("") }
    var scene by remember { mutableStateOf("The Moonlit Academy") }
    val messages = remember {
        mutableStateListOf(ChatLine("Aiko", "The academy is quiet tonight… ✨ I saved you a seat by the window. What adventure should we begin?", false))
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Violet, background = Ink, surface = SurfaceDark)) {
        Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding().navigationBarsPadding()) {
            Column(Modifier.fillMaxWidth().background(SurfaceDark).padding(18.dp)) {
                Text("ANIMEVERSE", color = Color(0xFFC4B5FD), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text("Your story. Your universe.", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text("AI roleplay • early preview", color = Color.LightGray, fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Aiko • Star Guardian", "Ren • Quiet Mage", "Mika • Rival Hero").forEach { name ->
                    FilterChip(selected = character == name, onClick = { character = name }, label = { Text(name.substringBefore(" •"), fontSize = 11.sp) })
                }
            }
            OutlinedTextField(value = scene, onValueChange = { scene = it }, label = { Text("Story world") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                items(messages) { line ->
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (line.mine) Alignment.End else Alignment.Start) {
                        Text(if (line.mine) "YOU" else line.speaker.uppercase(), color = Color(0xFFB9A5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Box(Modifier.widthIn(max = 320.dp).background(if (line.mine) Color(0xFF49357A) else SurfaceDark, RoundedCornerShape(16.dp)).padding(13.dp)) {
                            Text(line.text, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(1f).background(SurfaceDark, RoundedCornerShape(24.dp)).padding(15.dp), textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp), decorationBox = { inner -> if (draft.isEmpty()) Text("Write your next scene…", color = Color.Gray); inner() })
                Button(onClick = {
                    val prompt = draft.trim()
                    if (prompt.isNotEmpty()) {
                        messages.add(ChatLine("You", prompt, true))
                        val reply = if (prompt.contains("hello", true) || prompt.contains("hi", true)) "A warm smile appears. “I'm glad you're here. Tell me what happens next.”" else "Aiko considers your words, eyes bright with curiosity. “That changes everything… Let's find out where our story goes next.”"
                        messages.add(ChatLine(character.substringBefore(" •"), "($scene) $reply", false))
                        draft = ""
                    }
                }, shape = RoundedCornerShape(24.dp)) { Text("Send") }
            }
            Text("Preview build • Replies are currently scripted; connect an AI provider for live generation.", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp))
        }
    }
}
