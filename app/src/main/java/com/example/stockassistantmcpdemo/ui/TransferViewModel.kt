package com.example.stockassistantmcpdemo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockassistantmcpdemo.data.MCPApi
import com.example.stockassistantmcpdemo.data.NetworkModule
import com.example.stockassistantmcpdemo.data.StoreOption
import com.example.stockassistantmcpdemo.data.TransferAssistContext
import com.example.stockassistantmcpdemo.data.TransferRequest
import com.example.stockassistantmcpdemo.data.TransferSuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val CASE_SIZE = 12

data class SelectedTransferItem(
    val product: String,
    val singles: String,
    val cases: String,
    val sourceLabel: String,
    val reason: String? = null,
    val confidence: String? = null
)

data class TransferUiState(
    val fromStore: String = "101",
    val transferType: String = "PFS",
    val destinationQuery: String = "",
    val selectedDestination: StoreOption? = null,
    val storeOptions: List<StoreOption> = emptyList(),
    val productOptions: List<String> = emptyList(),
    val productQuery: String = "",
    val singles: String = "",
    val cases: String = "",
    val recommendations: List<TransferSuggestion> = emptyList(),
    val selectedItems: List<SelectedTransferItem> = emptyList(),
    val message: String = "",
    val isError: Boolean = false,
    val isLoading: Boolean = false
)

class TransferViewModel : ViewModel() {
    private val api: MCPApi = NetworkModule.provideApi()

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    private var pendingContext: TransferAssistContext? = null

    init {
        loadReferenceData()
    }

    fun applyAssistantContext(context: TransferAssistContext?) {
        if (context == null) return
        pendingContext = context
        applyPendingContextIfPossible()
    }

    fun updateFromStore(value: String) {
        _uiState.value = _uiState.value.copy(fromStore = value, message = "", isError = false)
    }

    fun updateTransferType(value: String) {
        _uiState.value = _uiState.value.copy(
            transferType = value.uppercase(),
            message = "",
            isError = false
        )
    }

    fun updateDestinationQuery(value: String) {
        _uiState.value = _uiState.value.copy(
            destinationQuery = value,
            selectedDestination = null,
            message = "",
            isError = false
        )
    }

    fun selectDestination(store: StoreOption) {
        _uiState.value = _uiState.value.copy(
            destinationQuery = "${store.store_name} (${store.store_id})",
            selectedDestination = store,
            message = "",
            isError = false
        )
    }

    fun updateProductQuery(value: String) {
        _uiState.value = _uiState.value.copy(productQuery = value, message = "", isError = false)
    }

    fun updateSingles(value: String) {
        _uiState.value = _uiState.value.copy(singles = value.filter { it.isDigit() })
    }

    fun updateCases(value: String) {
        _uiState.value = _uiState.value.copy(cases = value.filter { it.isDigit() })
    }

    fun updateSelectedSingles(product: String, value: String) {
        _uiState.value = _uiState.value.copy(
            selectedItems = _uiState.value.selectedItems.map { item ->
                if (item.product == product) item.copy(singles = value.filter { it.isDigit() }) else item
            }
        )
    }

    fun updateSelectedCases(product: String, value: String) {
        _uiState.value = _uiState.value.copy(
            selectedItems = _uiState.value.selectedItems.map { item ->
                if (item.product == product) item.copy(cases = value.filter { it.isDigit() }) else item
            }
        )
    }

    fun removeSelectedItem(product: String) {
        _uiState.value = _uiState.value.copy(
            selectedItems = _uiState.value.selectedItems.filterNot { it.product == product }
        )
    }

    fun fetchRecommendations() {
        fetchRecommendationsInternal()
    }

    private fun fetchRecommendationsInternal(successMessage: String? = null) {
        val state = _uiState.value
        val fromId = state.fromStore.toIntOrNull()
        val destination = resolveDestination(state)

        if (fromId == null || destination == null) {
            _uiState.value = state.copy(
                message = "Pick a source store and destination to load AI picks.",
                isError = true
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = "", isError = false)
            try {
                val response = api.getTransferRecommendations(fromId, destination.store_id, state.transferType)
                _uiState.value = _uiState.value.copy(
                    recommendations = response.suggestions,
                    isLoading = false,
                    message = successMessage ?: if (response.suggestions.isEmpty()) {
                        "No strong repeat pattern yet. You can still add items manually."
                    } else {
                        "AI picks are ready. Review them or add more items manually."
                    },
                    isError = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "I couldn’t load AI picks right now. You can still build the transfer manually.",
                    isError = true
                )
            }
        }
    }

    fun addRecommendedItem(item: TransferSuggestion) {
        mergeSelectedItem(
            SelectedTransferItem(
                product = item.product,
                singles = item.suggested_qty.toString(),
                cases = "0",
                sourceLabel = "Recommended",
                reason = item.reason,
                confidence = item.confidence
            )
        )
    }

    fun addManualItem() {
        val state = _uiState.value
        val singles = state.singles.toIntOrNull() ?: 0
        val cases = state.cases.toIntOrNull() ?: 0
        val totalQty = singles + (cases * CASE_SIZE)
        val rawProduct = state.productQuery.trim()

        if (rawProduct.isBlank()) {
            _uiState.value = state.copy(
                message = "Scan or type a product before adding it.",
                isError = true
            )
            return
        }

        if (totalQty <= 0) {
            _uiState.value = state.copy(
                message = "Add at least one single or case.",
                isError = true
            )
            return
        }

        val normalizedProduct = state.productOptions.firstOrNull {
            it.equals(rawProduct, ignoreCase = true)
        } ?: rawProduct

        mergeSelectedItem(
            SelectedTransferItem(
                product = normalizedProduct,
                singles = singles.toString(),
                cases = cases.toString(),
                sourceLabel = "Manual"
            )
        )

        _uiState.value = _uiState.value.copy(
            productQuery = "",
            singles = "",
            cases = "",
            message = "$normalizedProduct added to this transfer.",
            isError = false
        )
    }

    fun submitTransfer() {
        val state = _uiState.value
        val fromId = state.fromStore.toIntOrNull()
        val destination = resolveDestination(state)

        if (fromId == null || destination == null) {
            _uiState.value = state.copy(
                message = "Pick both source and destination stores before submitting.",
                isError = true
            )
            return
        }

        if (state.selectedItems.isEmpty()) {
            _uiState.value = state.copy(
                message = "Add at least one product before submitting.",
                isError = true
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = "", isError = false)
            var successCount = 0
            val failedProducts = mutableListOf<String>()

            for (item in _uiState.value.selectedItems) {
                val qty = item.totalQuantity()
                if (qty <= 0) {
                    failedProducts += item.product
                    continue
                }

                try {
                    val response = api.transferStock(
                        TransferRequest(
                            product_name = item.product,
                            from_store = fromId,
                            to_store = destination.store_id,
                            quantity = qty,
                            transfer_type = _uiState.value.transferType
                        )
                    )

                    if (response.ok) {
                        successCount += 1
                    } else {
                        failedProducts += item.product
                    }
                } catch (_: Exception) {
                    failedProducts += item.product
                }
            }

            val currentItems = _uiState.value.selectedItems
            val remainingItems = if (successCount > 0) {
                currentItems.filter { it.product in failedProducts }
            } else {
                currentItems
            }

            val message = when {
                successCount == currentItems.size -> {
                    "Transfer saved locally for $successCount items."
                }
                successCount > 0 -> {
                    "Saved $successCount items. Please review ${failedProducts.joinToString()}."
                }
                else -> {
                    "Couldn’t save this transfer. Check stock availability and try again."
                }
            }

            _uiState.value = _uiState.value.copy(
                selectedItems = remainingItems,
                isLoading = false,
                message = message,
                isError = successCount == 0
            )

            if (successCount > 0) {
                fetchRecommendationsInternal(message)
            }
        }
    }

    private fun loadReferenceData() {
        viewModelScope.launch {
            try {
                val stores = api.getStores().items
                val products = api.getProducts().items
                _uiState.value = _uiState.value.copy(
                    storeOptions = stores,
                    productOptions = products
                )
                applyPendingContextIfPossible()
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Reference data is unavailable right now.",
                    isError = true
                )
            }
        }
    }

    private fun applyPendingContextIfPossible() {
        val context = pendingContext ?: return
        val stores = _uiState.value.storeOptions
        val destination = stores.firstOrNull { it.store_id == context.toStore }

        var nextState = _uiState.value.copy(
            fromStore = context.fromStore?.toString() ?: _uiState.value.fromStore,
            transferType = context.transferType?.uppercase() ?: _uiState.value.transferType
        )

        if (destination != null) {
            nextState = nextState.copy(
                destinationQuery = "${destination.store_name} (${destination.store_id})",
                selectedDestination = destination
            )
        } else if (context.toStore != null) {
            nextState = nextState.copy(destinationQuery = context.toStore.toString())
        }

        _uiState.value = nextState

        if (!context.product.isNullOrBlank() && context.qty != null && context.qty > 0) {
            mergeSelectedItem(
                SelectedTransferItem(
                    product = context.product,
                    singles = context.qty.toString(),
                    cases = "0",
                    sourceLabel = "Assistant"
                )
            )
        }

        pendingContext = null

        if (_uiState.value.fromStore.toIntOrNull() != null && resolveDestination(_uiState.value) != null) {
            fetchRecommendations()
        }
    }

    private fun resolveDestination(state: TransferUiState): StoreOption? {
        state.selectedDestination?.let { return it }

        val exactId = state.destinationQuery.filter { it.isDigit() }.toIntOrNull()
        if (exactId != null) {
            return state.storeOptions.firstOrNull { it.store_id == exactId }
        }

        return state.storeOptions.firstOrNull {
            state.destinationQuery.equals(it.store_name, ignoreCase = true)
        }
    }

    private fun mergeSelectedItem(newItem: SelectedTransferItem) {
        val existing = _uiState.value.selectedItems.firstOrNull { it.product == newItem.product }
        val updatedItems = if (existing == null) {
            _uiState.value.selectedItems + newItem
        } else {
            _uiState.value.selectedItems.map { item ->
                if (item.product != newItem.product) {
                    item
                } else {
                    val nextSingles = (item.singles.toIntOrNull() ?: 0) + (newItem.singles.toIntOrNull() ?: 0)
                    val nextCases = (item.cases.toIntOrNull() ?: 0) + (newItem.cases.toIntOrNull() ?: 0)
                    item.copy(
                        singles = nextSingles.toString(),
                        cases = nextCases.toString(),
                        sourceLabel = if (item.sourceLabel == "Recommended" || newItem.sourceLabel == "Recommended") {
                            "Recommended"
                        } else {
                            newItem.sourceLabel
                        },
                        reason = item.reason ?: newItem.reason,
                        confidence = item.confidence ?: newItem.confidence
                    )
                }
            }
        }

        _uiState.value = _uiState.value.copy(selectedItems = updatedItems, isError = false)
    }

    private fun SelectedTransferItem.totalQuantity(): Int {
        val singlesQty = singles.toIntOrNull() ?: 0
        val caseQty = cases.toIntOrNull() ?: 0
        return singlesQty + (caseQty * CASE_SIZE)
    }
}
