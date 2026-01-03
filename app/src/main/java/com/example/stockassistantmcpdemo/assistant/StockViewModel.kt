package com.example.stockassistantmcpdemo.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockassistantmcpdemo.data.StockOverview
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsChannel
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

class StockViewModel : ViewModel() {
    private val _overview = MutableStateFlow<StockOverview?>(null)
    val overview = _overview.asStateFlow()

    private val baseUrl = "http://192.168.1.13:3000"

    private val _storeStock = MutableStateFlow<List<StoreStockItem>>(emptyList())
    val storeStock = _storeStock.asStateFlow()

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    @Serializable
    data class StoreStockItem(
        val storeId: Int,
        val product: String,
        val quantity: Int,
        val category: String?,
        val uom: String?,
        val price: Double?,
        val expiry_date: String?
    )

    @Serializable
    data class StoreStockResponse(val store_id: Int, val items: List<StoreStockItem>)


    suspend fun fetchOverview() {
        val data: StockOverview = client.get("$baseUrl/stock/overview").body()
        _overview.value = data
    }

    suspend fun fetchStoreStock(storeId: Int) {
        val data: StoreStockResponse = client.get("$baseUrl/stock/store/$storeId").body()
        _storeStock.value = data.items
    }

    fun listenLiveUpdates() {
        viewModelScope.launch {
            try {
                val client = HttpClient {
                    install(HttpTimeout)
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }

                client.get("http://192.168.1.13:3000/stock/live").bodyAsChannel().let { channel ->
                    val buffer = StringBuilder()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line( 4096) ?: continue
                        if (line.startsWith("data:")) {
                            val jsonLine = line.removePrefix("data:").trim()
                            val update = Json.decodeFromString<JsonObject>(jsonLine)

                            // 🔹 Trigger overview refresh
                            fetchOverview()

                            // 🔹 If storeId matches current selection, refresh store-level stock too
                            val storeId = update["store_id"]?.jsonPrimitive?.intOrNull
                            val currentStoreId = _storeStock.value.firstOrNull()?.storeId  // storeId in your data class
                            if (storeId != null && storeId == currentStoreId) {
                                fetchStoreStock(storeId)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("SSE error: ${e.message}")
            }
        }
    }

}
