package com.animeverse.ai

import android.content.Context
import com.tensai.llamakt.ChatMessage
import com.tensai.llamakt.LlamaEngine
import com.tensai.llamakt.SamplingParams
import com.tensai.llamakt.chat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * On-device inference runtime. The model is downloaded separately from the APK
 * and remains in app-private storage. No conversation data leaves the device
 * when Local mode is selected.
 */
class LocalAiRuntime(context: Context) {
    companion object {
        const val MODEL_FILE = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
        const val MODEL_URL = "https://huggingface.co/bartowski/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/Qwen2.5-0.5B-Instruct-Q4_K_M.gguf"
    }

    private val modelFile = File(File(context.filesDir, "animeverse-models"), MODEL_FILE)
    private val engine = LlamaEngine()
    @Volatile private var loaded = false

    fun isDownloaded(): Boolean = modelFile.exists() && modelFile.length() > 100_000_000L
    fun isLoaded(): Boolean = loaded
    fun modelPath(): String = modelFile.absolutePath

    suspend fun download(onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        if (isDownloaded()) {
            onProgress(1f)
            return@withContext
        }
        modelFile.parentFile?.mkdirs()
        val partial = File(modelFile.parentFile, modelFile.name + ".part")
        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30000
            readTimeout = 120000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "AnimeVerse-AI-Android")
        }
        try {
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("Model download failed (HTTP $code)")
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                partial.outputStream().buffered().use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    var received = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        received += count
                        if (total > 0) onProgress((received.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            if (partial.length() < 100_000_000L) throw IllegalStateException("Downloaded model file is incomplete.")
            if (modelFile.exists()) modelFile.delete()
            if (!partial.renameTo(modelFile)) throw IllegalStateException("Could not save the model file.")
            onProgress(1f)
        } catch (e: Exception) {
            partial.delete()
            throw e
        } finally {
            connection.disconnect()
        }
    }

    suspend fun load(onProgress: (Float) -> Unit = {}) = withContext(Dispatchers.IO) {
        if (!isDownloaded()) throw IllegalStateException("Download the local model first.")
        if (!loaded) {
            engine.load(path = modelFile.absolutePath, nGpuLayers = 0, nCtx = 2048, nThreads = 2,
                onProgress = com.tensai.llamakt.LoadProgressCallback { progress ->
                    onProgress(progress)
                    true
                })
            loaded = true
        }
    }

    suspend fun generate(systemPrompt: String, history: List<ChatMessage>): String =
        withContext(Dispatchers.Default) {
            if (!loaded) throw IllegalStateException("Local AI model is not loaded.")
            val messages = listOf(ChatMessage("system", systemPrompt)) + history
            val result = StringBuilder()
            engine.chat(
                messages = messages,
                params = SamplingParams(nPredict = 420, temperature = 0.85f, topK = 40, topP = 0.92f)
            ).collect { token -> result.append(token) }
            result.toString().trim().ifBlank { throw IllegalStateException("Local AI returned an empty reply.") }
        }

    fun close() {
        if (loaded) {
            engine.free()
            loaded = false
        }
    }
}
