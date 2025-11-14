package com.example.stockassistantmcpdemo.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Represents an item with low stock.
 *
 * @param product The name of the product.
 * @param qty The quantity of the product.
 */
data class LowStockItem(val product: String, val qty: Int)

/**
 * Represents the response from the server when fetching low stock items.
 *
 * @param store_id The ID of the store.
 * @param low_stock_items A list of items with low stock.
 */
data class LowStockResponse(val store_id: Int, val low_stock_items: List<LowStockItem>)

/**
 * Represents a request to transfer stock from one store to another.
 *
 * @param product_name The name of the product to transfer.
 * @param from_store The ID of the store to transfer from.
 * @param to_store The ID of the store to transfer to.
 * @param quantity The quantity of the product to transfer.
 */
data class TransferRequest(
    val product_name: String,
    val from_store: Int,
    val to_store: Int,
    val quantity: Int
)

/**
 * Represents the response from the server after a stock transfer.
 *
 * @param ok Whether the transfer was successful.
 * @param detail A message detailing the result of the transfer.
 */
data class TransferResponse(val ok: Boolean, val detail: String)

/**
 * An interface for the MCP API.
 *
 * This interface defines the API endpoints for getting low stock items and transferring stock.
 */
interface MCPApi {
    /**
     * Gets the low stock items for a given store.
     *
     * @param storeId The ID of the store to get low stock items for.
     * @param threshold The threshold for low stock.
     * @return A [LowStockResponse] containing the low stock items.
     */
    @GET("/tool/get_low_stock")
    suspend fun getLowStock(
        @Query("store_id") storeId: Int,
        @Query("threshold") threshold: Int = 10
    ): LowStockResponse

    /**
     * Transfers stock from one store to another.
     *
     * @param body A [TransferRequest] containing the details of the transfer.
     * @return A [TransferResponse] detailing the result of the transfer.
     */
    @POST("/tool/transfer_stock")
    suspend fun transferStock(@Body body: TransferRequest): TransferResponse
}

/**
 * A module for providing the [MCPApi] interface.
 */
object NetworkModule {
    // Use 10.0.2.2 to reach host machine when running on emulator.
    private const val BASE_URL = "http://192.168.1.5:3100"

    /**
     * Provides an instance of the [MCPApi] interface.
     *
     * @return An instance of the [MCPApi] interface.
     */
    fun provideApi(): MCPApi {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BASIC
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MCPApi::class.java)
    }
}
