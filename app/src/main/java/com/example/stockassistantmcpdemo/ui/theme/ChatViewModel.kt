package com.example.stockassistantmcpdemo.ui.theme

    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.example.stockassistantmcpdemo.data.NetworkModule
    import com.example.stockassistantmcpdemo.data.TransferRequest
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.launch

    data class Message(val sender: String, val text: String)

    class ChatViewModel : ViewModel() {
        private val api = NetworkModule.provideApi()
        private val _messages = MutableStateFlow<List<Message>>(emptyList())
        val messages: StateFlow<List<Message>> = _messages

        fun sendMessage(userText: String) {
            _messages.value = _messages.value + Message("You", userText)
            viewModelScope.launch {
                val reply = handleIntent(userText)
                _messages.value = _messages.value + Message("Bot", reply)
            }
        }

        private suspend fun handleIntent(text: String): String {
            // Simple intent parsing; can be replaced with LLM call later.
            return when {
                text.contains("low", ignoreCase = true) && text.contains("store", ignoreCase = true) -> {
                    val storeId = Regex("\\d+").find(text)?.value?.toIntOrNull() ?: 101
                    try {
                        val res = api.getLowStock(storeId)
                        if (res.low_stock_items.isEmpty()) "No low stock items for store $storeId."
                        else res.low_stock_items.joinToString("\n") { "${it.product}: ${it.qty}" }
                    } catch (e: Exception) {
                        "Error fetching low stock: ${e.message}"
                    }
                }
                text.contains("transfer", ignoreCase = true) -> {
                    // naive defaults; user should implement parsing or use LLM to extract entities
                    val req = TransferRequest("Bread", 101, 103, 2)
                    try {
                        val res = api.transferStock(req)
                        res.detail
                    } catch (e: Exception) {
                        "Error performing transfer: ${e.message}"
                    }
                }
                else -> "I can help with inventory checks (e.g., 'low stock for store 103') or transfers."
            }
        }
    }
