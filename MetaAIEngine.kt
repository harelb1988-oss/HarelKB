package com.harelkb.keyboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Adapter for a configured, OpenAI-compatible chat-completions endpoint.
 * This is NOT a claim of direct access to Meta AI. Configure a provider endpoint/key
 * you are authorized to use. Empty endpoint/key safely falls back to LocalEngine.
 */
class MetaAIEngine(
    private val apiKey: String = BuildConfig.AI_API_KEY,
    private val endpoint: String = BuildConfig.AI_ENDPOINT,
    private val local: LocalEngine = LocalEngine()
) {
    private val systemPrompt =
        "You are HarelKB AI, living inside keyboard. User is Harel, male, Hebrew. " +
        "Help with prediction, translation, rewriting, summarization."

    suspend fun getPredictions(ctx: String): List<String> {
        val answer = request("Suggest exactly three short next-word or next-phrase predictions. Context: $ctx")
            ?: return local.getPredictions(ctx)
        return answer.lines().map { it.trim().removePrefix("-").trim() }
            .filter { it.isNotBlank() }.take(3).ifEmpty { local.getPredictions(ctx) }
    }

    suspend fun translate(text: String, target: String): String =
        request("Translate the following text to $target. Return only the translation:\n$text")
            ?: local.translate(text, target)

    suspend fun rewrite(text: String, tone: String): String =
        request("Rewrite the following text in a $tone tone. Preserve meaning:\n$text")
            ?: local.rewrite(text, tone)

    suspend fun askAI(query: String): String =
        request(query) ?: local.askAI(query)

    suspend fun completeSentence(text: String): String =
        request("Continue this sentence naturally. Return only the continuation:\n$text")
            ?: local.completeSentence(text)

    private suspend fun request(prompt: String): String? = withContext(Dispatchers.IO) {
        if (endpoint.isBlank() || apiKey.isBlank()) return@withContext null
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
            val body = JSONObject().apply {
                put("model", "configured-model")
                put("messages", org.json.JSONArray().put(
                    JSONObject().put("role", "system").put("content", systemPrompt)
                ).put(JSONObject().put("role", "user").put("content", prompt)))
                put("temperature", 0.4)
            }.toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) return@withContext null
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(response).optJSONArray("choices")
                ?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                ?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
