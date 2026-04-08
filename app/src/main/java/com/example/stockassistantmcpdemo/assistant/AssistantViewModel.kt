package com.example.stockassistantmcpdemo.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockassistantmcpdemo.BuildConfig
import com.example.stockassistantmcpdemo.data.TransferAssistContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AssistantViewModel : ViewModel() {

    data class Message(
        val sender: String,
        val text: String,
        val responseMode: String? = null
    )

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _fullScreen = MutableStateFlow(true)
    val fullScreen: StateFlow<Boolean> = _fullScreen.asStateFlow()

    private val _pendingTransferContext = MutableStateFlow<TransferAssistContext?>(null)
    val pendingTransferContext: StateFlow<TransferAssistContext?> = _pendingTransferContext.asStateFlow()

    private val _lastResponseMode = MutableStateFlow<String?>(null)
    val lastResponseMode: StateFlow<String?> = _lastResponseMode.asStateFlow()

    private val client = OkHttpClient()
    private var currentStoreContext: Int? = null

    fun toggleMode() {
        _fullScreen.value = !_fullScreen.value
    }

    fun consumeTransferContext() {
        _pendingTransferContext.value = null
    }

    fun setCurrentStore(storeId: Int?) {
        currentStoreContext = storeId
    }

    fun send(text: String, currentStore: Int? = currentStoreContext) {
        if (text.isBlank()) return

        _messages.value = _messages.value + Message("You", text)
        _isTyping.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payloadBody = JSONObject()
                    .put("message", text)
                    .put("session_id", "android-demo")

                currentStore?.let { payloadBody.put("current_store", it) }

                val body = payloadBody
                    .toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("$BASE_URL/chat")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    val payload = JSONObject(raw)

                    val rawReply = payload.optString("reply")
                    val responseMode = payload.optString("response_mode").ifBlank { null }
                    val reply = when {
                        rawReply.isBlank() -> "Ready."
                        rawReply.trim() == "<natural reply>" -> "Ready. Try PFS 201, PFS 204, or canteen."
                        else -> rawReply
                    }
                    appendBotMessage(reply, responseMode)

                    if (payload.optString("action") == "open_transfer_assist") {
                        payload.optJSONObject("transfer_context")?.let { context ->
                            _pendingTransferContext.value = TransferAssistContext(
                                fromStore = context.optInt("from_store").takeIf { it != 0 },
                                toStore = context.optInt("to_store").takeIf { it != 0 },
                                transferType = context.optString("transfer_type").ifBlank { null },
                                product = context.optString("product").ifBlank { null },
                                qty = context.optInt("qty").takeIf { it != 0 }
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                appendBotMessage(
                    "I couldn’t reach the local stock demo just now. Please try again in a moment.",
                    "offline"
                )
            } finally {
                _isTyping.value = false
            }
        }
    }

    fun quickLowStock() {
        send("Show low stock for store 103")
    }

    fun quickTransferPlan() {
        send("What should I transfer from store 101 to the staff canteen?")
    }

    fun quickScanStub() {
        send("scan 8901234567890")
    }

    private fun appendBotMessage(text: String, responseMode: String? = null) {
        _lastResponseMode.value = responseMode
        _messages.value = _messages.value + Message("Bot", text, responseMode)
    }

    companion object {
        val BASE_URL = BuildConfig.ASSISTANT_BASE_URL
    }
}
