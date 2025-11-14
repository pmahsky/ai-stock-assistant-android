package com.example.stockassistantmcpdemo.data

import kotlinx.serialization.Serializable

@Serializable
data class StockOverview(
    val total_items: Int,
    val total_quantity: Int,
    val low_stock: Int,
    val expiring: Int
)
