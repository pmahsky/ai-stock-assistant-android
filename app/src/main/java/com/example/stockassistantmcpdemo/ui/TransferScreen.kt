package com.example.stockassistantmcpdemo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockassistantmcpdemo.data.StoreOption
import com.example.stockassistantmcpdemo.data.TransferAssistContext
import com.example.stockassistantmcpdemo.data.TransferSuggestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    onBack: () -> Unit,
    initialContext: TransferAssistContext? = null,
    vm: TransferViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()

    LaunchedEffect(initialContext) {
        vm.applyAssistantContext(initialContext)
    }

    val filteredStores = remember(state.destinationQuery, state.storeOptions) {
        val query = state.destinationQuery.lowercase()
        if (query.isBlank()) {
            state.storeOptions
        } else {
            state.storeOptions.filter { store ->
                query in store.store_name.lowercase() ||
                    query in store.store_type.lowercase() ||
                    query in store.store_id.toString()
            }
        }
    }

    val filteredProducts = remember(state.productQuery, state.productOptions) {
        val query = state.productQuery.lowercase()
        if (query.isBlank()) {
            emptyList()
        } else {
            state.productOptions.filter { it.lowercase().contains(query) }.take(4)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer Assist") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
            Text(
                "Use AI picks for frequent PFS or canteen transfers, then add or adjust products before saving.",
                style = MaterialTheme.typography.bodyMedium
            )
            }

            item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.fromStore,
                    onValueChange = vm::updateFromStore,
                    label = { Text("Source Store") },
                    modifier = Modifier.weight(0.9f),
                    singleLine = true
                )
                Button(
                    onClick = { vm.fetchRecommendations() },
                    modifier = Modifier.weight(1.1f),
                    enabled = !state.isLoading
                ) {
                    Text("Refresh AI Picks")
                }
            }
            }

            item {
            TransferTypeChips(
                selectedType = state.transferType,
                onTypeSelected = vm::updateTransferType
            )
            }

            item {
                OutlinedTextField(
                    value = state.destinationQuery,
                    onValueChange = vm::updateDestinationQuery,
                    label = { Text("Transfer To Store / PFS / Canteen") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            if (filteredStores.isNotEmpty() && state.destinationQuery.isNotBlank() && state.selectedDestination == null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredStores.take(5).forEach { store ->
                            StoreSuggestionCard(
                                store = store,
                                onSelect = { vm.selectDestination(store) }
                            )
                        }
                    }
                }
            }

            item {
            Text("Add Product", style = MaterialTheme.typography.titleMedium)
            }

            item {
            OutlinedTextField(
                value = state.productQuery,
                onValueChange = vm::updateProductQuery,
                label = { Text("Scan or Search Product") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            }

            if (filteredProducts.isNotEmpty()) {
                item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredProducts.forEach { product ->
                        AssistChip(
                            onClick = { vm.updateProductQuery(product) },
                            label = { Text(product) }
                        )
                    }
                }
                }
            }

            item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.singles,
                    onValueChange = vm::updateSingles,
                    label = { Text("Singles") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.cases,
                    onValueChange = vm::updateCases,
                    label = { Text("Cases") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = { vm.addManualItem() },
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text("Add")
                }
            }
            }

            item {
            Text(
                "Demo note: 1 case = 12 singles.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            }

            if (state.message.isNotBlank()) {
                item {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isError) Color(0xFFB3261E) else Color(0xFF1B5E20)
                )
                }
            }

            item {
                SectionHeader("AI Recommendations")
            }

            if (state.recommendations.isEmpty()) {
                item {
                    Text(
                        "Choose the source and destination, then refresh AI picks. If there’s no strong pattern, add products manually.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            } else {
                items(state.recommendations) { suggestion ->
                    RecommendationCard(
                        item = suggestion,
                        onAdd = { vm.addRecommendedItem(suggestion) }
                    )
                }
            }

            item {
                SectionHeader("Products In This Transfer")
            }

            if (state.selectedItems.isEmpty()) {
                item {
                    Text(
                        "No products added yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            } else {
                items(state.selectedItems) { item ->
                    SelectedItemCard(
                        item = item,
                        onSinglesChange = { vm.updateSelectedSingles(item.product, it) },
                        onCasesChange = { vm.updateSelectedCases(item.product, it) },
                        onRemove = { vm.removeSelectedItem(item.product) }
                    )
                }
            }

            item {
                Button(
                    onClick = { vm.submitTransfer() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                ) {
                    Text(if (state.isLoading) "Saving..." else "Submit Transfer")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
private fun StoreSuggestionCard(store: StoreOption, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(store.store_name, fontWeight = FontWeight.Medium)
                Text(
                    "${store.store_type} • ${store.store_id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            TextButton(onClick = onSelect) {
                Text("Select")
            }
        }
    }
}

@Composable
private fun TransferTypeChips(selectedType: String, onTypeSelected: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("PFS", "CANTEEN").forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(type) }
            )
        }
    }
}

@Composable
private fun RecommendationCard(item: TransferSuggestion, onAdd: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.product, style = MaterialTheme.typography.titleMedium)
                AssistChip(
                    onClick = {},
                    label = { Text(item.confidence.replaceFirstChar { it.uppercase() }) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Suggested qty ${item.suggested_qty} • score ${"%.2f".format(item.score)} • moved ${item.frequency} times",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.reason, style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAdd) {
                Text("Add To Transfer")
            }
        }
    }
}

@Composable
private fun SelectedItemCard(
    item: SelectedTransferItem,
    onSinglesChange: (String) -> Unit,
    onCasesChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(item.product, style = MaterialTheme.typography.titleMedium)
                    Text(
                        buildString {
                            append(item.sourceLabel)
                            item.confidence?.let { append(" • $it confidence") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                TextButton(onClick = onRemove) {
                    Text("Remove")
                }
            }

            item.reason?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.singles,
                    onValueChange = onSinglesChange,
                    label = { Text("Singles") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = item.cases,
                    onValueChange = onCasesChange,
                    label = { Text("Cases") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}
