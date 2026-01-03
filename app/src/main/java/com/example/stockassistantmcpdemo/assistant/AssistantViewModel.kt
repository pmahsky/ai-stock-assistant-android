package com.example.stockassistantmcpdemo.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.InputStreamReader

/**
 * ViewModel for managing the AI assistant's state and interactions.
 *
 * This ViewModel handles the chat's message history, typing indicators, and communication with the backend.
 * It provides methods for sending messages, including both streaming and non-streaming options, and for
 * triggering quick actions like checking low stock or initiating a transfer.
 */
class AssistantViewModel : ViewModel() {

    /**
     * Represents a single message in the chat.
     */
    data class Message(val sender: String, val text: String)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    /**
     * A StateFlow that emits the latest list of chat messages.
     */
    val messages: StateFlow<List<Message>> = _messages

    private val _isTyping = MutableStateFlow(false)
    /**
     * A StateFlow that emits whether the bot is currently typing.
     */
    val isTyping: StateFlow<Boolean> = _isTyping

    // UI mode toggle: full-screen vs bottom sheet
    private val _fullScreen = MutableStateFlow(true)
    val fullScreen: StateFlow<Boolean> = _fullScreen
    fun toggleMode() { _fullScreen.value = !_fullScreen.value }

    private val client = OkHttpClient()

    /**
     * Sends a message to the bot.
     *
     * @param text The text of the message to send.
     */
    fun send(text: String) {
        if (text.isBlank()) return
        // Add user message
        _messages.value = _messages.value + Message("You", text)
        // Use streaming by default
        sendStreaming(text)
    }

    /**
     * Sends a pre-defined message to the bot to show low stock for a specific store.
     */
    fun quickLowStock() {
        send("show low stock for store 103")
    }

    /**
     * Sends a pre-defined message to the bot to transfer a product between stores.
     */
    fun quickTransfer() {
        send("transfer 2 bread from store 101 to 103")
    }

    /**
     * Sends a pre-defined message to the bot to simulate a barcode scan.
     */
    fun quickScanStub() {
        // you can replace this with real barcode scan → send("scan:<code>")
        send("scan 8901234567890")
    }

    /**
     * Calls the non-streaming /chat endpoint.
     *
     * @param text The text of the message to send.
     */
    fun sendOnce(text: String) {
        _isTyping.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject()
                    .put("message", text)
                    .put("session_id", "android-demo")
                val body = json.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val req = Request.Builder()
                    .url("$BASE_URL/chat")
                    .post(body)
                    .build()

                client.newCall(req).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    val reply = try {
                        JSONObject(raw).optString("reply", "...")
                    } catch (_: Exception) {
                        raw.ifBlank { "..." }
                    }
                    appendBotMessage(reply)
                }
            } catch (e: Exception) {
                appendBotMessage("Error: ${e.message ?: "unknown"}")
            } finally {
                _isTyping.value = false
            }
        }
    }

    /**
     * Calls the streaming /chat_stream endpoint and appends chunks as they arrive.
     *
     * @param text The text of the message to send.
     */
    fun sendStreaming(text: String) {
        _isTyping.value = true
        // Insert an empty bot message we’ll stream into
        _messages.value = _messages.value + Message("Bot", "")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject()
                    .put("message", text)
                    .put("session_id", "android-demo")
                val body = json.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val req = Request.Builder()
                    .url("$BASE_URL/chat_stream")
                    .post(body)
                    .build()

                client.newCall(req).execute().use { resp ->
                    val reader = InputStreamReader(resp.body?.byteStream(), Charsets.UTF_8)
                    val buf = CharArray(1024)
                    while (true) {
                        val n = reader.read(buf)
                        if (n == -1) break
                        val delta = String(buf, 0, n)

                        appendToLastBotMessage(delta)

                    }
                }
            } catch (e: Exception) {
                appendToLastBotMessage(
                    "\n⚠️ Could not reach stock server.\n" +
                            "Make sure phone & laptop are on same Wi-Fi.\n" +
                            "Try: http://<your-laptop-IP>:3100"
                )
            } finally {
                _isTyping.value = false
            }
        }
    }

    private fun appendBotMessage(text: String) {
        _messages.value = _messages.value + Message("Bot", text)
    }

    private fun appendToLastBotMessage(delta: String) {
        val list = _messages.value
        if (list.isEmpty()) {
            _messages.value = list + Message("Bot", delta)
            return
        }
        val last = list.last()
        if (last.sender != "Bot") {
            _messages.value = list + Message("Bot", delta)
            return
        }
        _messages.value = list.dropLast(1) + last.copy(text = last.text + delta)
    }

    companion object {
        // Emulator: 10.0.2.2 — for physical device replace with your laptop IP
        const val BASE_URL = "http://192.168.1.13:3000"
    }
}
