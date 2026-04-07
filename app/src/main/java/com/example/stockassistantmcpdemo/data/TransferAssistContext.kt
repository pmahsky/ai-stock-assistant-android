package com.example.stockassistantmcpdemo.data

data class TransferAssistContext(
    val fromStore: Int? = null,
    val toStore: Int? = null,
    val transferType: String? = null,
    val product: String? = null,
    val qty: Int? = null
)
