package com.example.taaltrails

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

data class ChatMsg(val me: Boolean, val text: String, val picks: List<Place> = emptyList())

object Gemini {
    // To try another model, change only this line.
    private const val MODEL = "gemini-3.6-flash"

    val configured: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    private fun systemPrompt(): String {
        val spots = PLACES.joinToString("\n") { p ->
            "- ${p.name} (${p.tag}, ${p.kind.name.lowercase()}). ${p.desc} " +
                    "Address: ${p.address}. Suggested visit: ${p.window}. " +
                    "Price: ${p.price}. Rating: ${p.rating}."
        }
        return "You are Taal Guide, the friendly assistant inside a travel app " +
                "about Taal, Batangas, Philippines. You can chat about anything the " +
                "user asks, including general questions, but you are especially " +
                "helpful about Taal: its heritage, food, crafts, getting there, " +
                "weather and trip planning.\n\n" +
                "Style: warm and concise, usually under 120 words, plain text only. " +
                "Do not use markdown such as asterisks, pound signs or tables. Short " +
                "lists with simple dashes are fine.\n\n" +
                "Honesty: if you are not sure of a fact, say so. Never invent prices, " +
                "opening times or phone numbers. For anything that can change, such " +
                "as the volcano alert level, transport schedules or opening hours, " +
                "tell the user to check the latest official source. For volcano " +
                "safety, mention PHIVOLCS.\n\n" +
                "The app has these curated places. When they fit the question, " +
                "recommend them by their exact names:\n$spots"
    }

    suspend fun ask(msgs: List<ChatMsg>): Result<String> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            // Skip the app greeting; the first message sent must come from the user.
            val recent = msgs.drop(1).takeLast(12)
            val start = recent.indexOfFirst { it.me }
            if (start < 0) return@withContext Result.failure(Exception("no question"))

            val contents = JSONArray()
            recent.drop(start).forEach { m ->
                contents.put(
                    JSONObject()
                        .put("role", if (m.me) "user" else "model")
                        .put("parts", JSONArray().put(JSONObject().put("text", m.text)))
                )
            }

            val body = JSONObject()
                .put(
                    "system_instruction",
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt())))
                )
                .put("contents", contents)
                .put("generationConfig", JSONObject().put("maxOutputTokens", 2048))

            val c = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent")
                .openConnection() as HttpURLConnection
            conn = c
            c.requestMethod = "POST"
            c.connectTimeout = 30000
            c.readTimeout = 90000
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = c.responseCode
            if (code != 200) {
                // Read Google's own explanation so the chat can show the real reason.
                val errText = try {
                    c.errorStream?.bufferedReader()?.use { it.readText() }
                } catch (e: Exception) {
                    null
                }
                val detail = try {
                    JSONObject(errText ?: "").getJSONObject("error").optString("message")
                } catch (e: Exception) {
                    ""
                }
                return@withContext Result.failure(Exception("error $code ${detail.take(140)}".trim()))
            }

            val raw = c.inputStream.bufferedReader().use { it.readText() }
            val parts = JSONObject(raw)
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?: return@withContext Result.failure(Exception("empty reply"))

            val sb = StringBuilder()
            for (i in 0 until parts.length()) sb.append(parts.getJSONObject(i).optString("text"))

            if (sb.isBlank()) Result.failure(Exception("empty reply"))
            else Result.success(sb.toString())
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("timed out"))
        } catch (e: UnknownHostException) {
            Result.failure(Exception("no internet on this device"))
        } catch (e: Exception) {
            Result.failure(Exception("${e.javaClass.simpleName}: ${e.message}".take(140)))
        } finally {
            conn?.disconnect()
        }
    }

    /** Places named in the AI's answer, shown as tappable cards. */
    fun mentioned(text: String): List<Place> {
        val t = text.lowercase()
        return PLACES.filter { t.contains(it.name.lowercase()) }.take(4)
    }
}