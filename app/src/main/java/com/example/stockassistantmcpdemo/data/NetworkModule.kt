package com.example.stockassistantmcpdemo.data

import com.example.stockassistantmcpdemo.BuildConfig
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
/**
 * Represents a request to transfer stock from one store to another.
 *
 * @param product_name The name of the product to transfer.
 * @param from_store The ID of the store to transfer from.
 * @param to_store The ID of the store to transfer to.
 * @param quantity The quantity of the product to transfer.
 * @param transfer_type The type of transfer (e.g. "MANUAL", "PFS", "CANTEEN").
 */
data class TransferRequest(
    val product_name: String,
    val from_store: Int,
    val to_store: Int,
    val quantity: Int,
    val transfer_type: String = "MANUAL"
)

/**
 * Represents the response from the server after a stock transfer.
 *
 * @param ok Whether the transfer was successful.
 * @param detail A message detailing the result of the transfer.
 */
data class TransferResponse(val ok: Boolean, val detail: String)

/**
 * Represents a product suggestion for transfer.
 */
data class TransferSuggestion(
    val product: String,
    val suggested_qty: Int,
    val frequency: Int,
    val score: Double,
    val confidence: String,
    val reason: String
)

data class StoreOption(
    val store_id: Int,
    val store_name: String,
    val store_type: String,
    val parent_store_id: Int? = null
)

data class StoreDirectoryResponse(val items: List<StoreOption>)

data class ProductListResponse(val items: List<String>)

/**
 * Represents the response containing transfer recommendations.
 */
data class TransferRecommendationResponse(
    val from_store: Int,
    val to_store: Int,
    val transfer_type: String,
    val suggestions: List<TransferSuggestion>
)

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
    @GET("/low_stock/{store_id}")
    suspend fun getLowStock(
        @Path("store_id") storeId: Int,
        @Query("threshold") threshold: Int = 10
    ): LowStockResponse

    /**
     * Transfers stock from one store to another.
     *
     * @param body A [TransferRequest] containing the details of the transfer.
     * @return A [TransferResponse] detailing the result of the transfer.
     */
    @POST("/transfer_stock")
    suspend fun transferStock(@Body body: TransferRequest): TransferResponse

    /**
     * Gets transfer recommendations based on history.
     */
    @GET("/transfer_recommendations")
    suspend fun getTransferRecommendations(
        @Query("from_store") fromStore: Int,
        @Query("to_store") toStore: Int,
        @Query("transfer_type") transferType: String
    ): TransferRecommendationResponse

    @GET("/stores")
    suspend fun getStores(@Query("q") query: String? = null): StoreDirectoryResponse

    @GET("/products")
    suspend fun getProducts(@Query("q") query: String? = null): ProductListResponse
}

/**
 * A module for providing the [MCPApi] interface.
 */
object NetworkModule {
    private val BASE_URL = BuildConfig.BACKEND_BASE_URL

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
