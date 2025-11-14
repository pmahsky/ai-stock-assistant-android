package com.example.stockassistantmcpdemo.assistant

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stockassistantmcpdemo.data.StockOverview

@Composable
fun StockOverviewCard(overview: StockOverview) {
    Card(Modifier.fillMaxWidth().padding(8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("📦 Stock Overview", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Total Items: ${overview.total_items}")
            Text("Total Quantity: ${overview.total_quantity}")
            Text("Low Stock: ${overview.low_stock}")
            Text("Expiring Soon: ${overview.expiring}")
        }
    }
}