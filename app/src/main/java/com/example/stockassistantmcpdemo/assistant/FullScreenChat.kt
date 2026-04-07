package com.example.stockassistantmcpdemo.assistant

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockassistantmcpdemo.data.TransferAssistContext
import com.example.stockassistantmcpdemo.data.TransferSuggestion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenChat(
    vm: AssistantViewModel,
    paddingValues: PaddingValues,
    onOpenScanner: () -> Unit,
    onOpenTransfer: (TransferAssistContext?) -> Unit
) {
    val messages by vm.messages.collectAsState()
    val stockVM: StockViewModel = viewModel()
    val overview by stockVM.overview.collectAsState()
    val storeStock by stockVM.storeStock.collectAsState()
    val pfsHighlights by stockVM.pfsHighlights.collectAsState()
    val canteenHighlights by stockVM.canteenHighlights.collectAsState()

    var input by remember { mutableStateOf("") }
    var selectedStore by remember { mutableIntStateOf(101) }
    val storeOptions = listOf(101, 102, 103)

    val isTyping by vm.isTyping.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var ttsEnabled by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(Unit) {
        tts = TextToSpeech(context, { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1f)
                Log.d("TTS_DEBUG", "TTS ready")
            }
        }, "com.google.android.tts")
        stockVM.fetchOverview()
        stockVM.fetchTransferHighlights()
        stockVM.listenLiveUpdates()
    }

    LaunchedEffect(selectedStore) {
        stockVM.fetchStoreStock(selectedStore)
    }

    DisposableEffect(Unit) {
        onDispose { tts?.shutdown() }
    }

    LaunchedEffect(messages) {
        if (!ttsEnabled) {
            tts?.stop()
            return@LaunchedEffect
        }

        val last = messages.lastOrNull() ?: return@LaunchedEffect
        if (last.sender == "You") return@LaunchedEffect

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        tts?.speak(last.text, TextToSpeech.QUEUE_FLUSH, params, "AI_RESPONSE")
    }

    LaunchedEffect(messages.size, isTyping) {
        if (listState.layoutInfo.totalItemsCount > 0) {
            scope.launch {
                listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Plan transfers, answer stock questions, and guide new teammates.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = {
                ttsEnabled = !ttsEnabled
                if (!ttsEnabled) tts?.stop()
            }) {
                Icon(
                    if (ttsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Toggle TTS"
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            item {
                HeroCard(
                    onAskCapabilities = { vm.send("I am new here. What can you do?") },
                    onExplainTransfer = { vm.send("How does Transfer Assist work?") },
                    onOpenTransfer = { onOpenTransfer(null) }
                )
            }

            item {
                SectionHeader("Starter Prompts")
            }

            item {
                PromptChipRow(
                    prompts = listOf(
                        "Capabilities" to "What can you do?",
                        "PFS 204 picks" to "Suggest products for PFS 204",
                        "Canteen plan" to "What should I transfer to staff canteen?",
                        "Low stock 103" to "Show low stock in store 103"
                    ),
                    onPromptClick = vm::send
                )
            }

            overview?.let {
                item {
                    SectionHeader("Shift Briefing")
                }
                item {
                    MetricsOverviewCard(
                        totalItems = it.total_items,
                        totalQuantity = it.total_quantity,
                        lowStock = it.low_stock,
                        expiring = it.expiring
                    )
                }
            }

            item {
                SectionHeader("Recommended Runs")
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RunPlanCard(
                        title = "PFS 204 Replenishment",
                        subtitle = "Best repeat items from Parent Store 101",
                        suggestions = pfsHighlights,
                        emptyLabel = "No repeat pattern yet for PFS 204.",
                        onAsk = { vm.send("Suggest products for PFS 204") },
                        onOpen = {
                            onOpenTransfer(
                                TransferAssistContext(
                                    fromStore = 101,
                                    toStore = 204,
                                    transferType = "PFS"
                                )
                            )
                        }
                    )
                    RunPlanCard(
                        title = "Staff Canteen Transfer",
                        subtitle = "Likely next run from Parent Store 101",
                        suggestions = canteenHighlights,
                        emptyLabel = "No repeat pattern yet for staff canteen.",
                        onAsk = { vm.send("What should I transfer to staff canteen?") },
                        onOpen = {
                            onOpenTransfer(
                                TransferAssistContext(
                                    fromStore = 101,
                                    toStore = 301,
                                    transferType = "CANTEEN"
                                )
                            )
                        }
                    )
                }
            }

            item {
                SectionHeader("Inventory Glance")
            }

            item {
                StoreInventoryCard(
                    selectedStore = selectedStore,
                    storeOptions = storeOptions,
                    storeStock = storeStock,
                    onStoreSelect = { selectedStore = it }
                )
            }

            item {
                SectionHeader("Conversation")
            }

            if (messages.isEmpty()) {
                item {
                    EmptyConversationCard(
                        onAskPrompt = { vm.send("What can you do?") }
                    )
                }
            } else {
                items(messages) { msg ->
                    MessageBubble(msg = msg)
                }
            }

            if (isTyping) {
                item { TypingDots() }
            }
        }

        Surface(
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask stock, transfers, or how to use the app…") },
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (input.isNotBlank()) {
                            vm.send(input.trim())
                            input = ""
                        }
                    }
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    onAskCapabilities: () -> Unit,
    onExplainTransfer: () -> Unit,
    onOpenTransfer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("New here?", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Use the assistant to learn the workflow, then jump into structured screens when you’re ready.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onAskCapabilities) {
                        Text("Capabilities")
                    }
                    FilledTonalButton(onClick = onExplainTransfer) {
                        Text("Explain Transfer")
                    }
                }
                Button(onClick = onOpenTransfer) {
                    Text("Open Transfer Assist")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun PromptChipRow(prompts: List<Pair<String, String>>, onPromptClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        prompts.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { (label, prompt) ->
                    AssistChip(
                        onClick = { onPromptClick(prompt) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricsOverviewCard(
    totalItems: Int,
    totalQuantity: Int,
    lowStock: Int,
    expiring: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Items", totalItems.toString(), Modifier.weight(1f))
                MetricTile("Quantity", totalQuantity.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Low Stock", lowStock.toString(), Modifier.weight(1f))
                MetricTile("Expiring", expiring.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun RunPlanCard(
    title: String,
    subtitle: String,
    suggestions: List<TransferSuggestion>,
    emptyLabel: String,
    onAsk: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (suggestions.isEmpty()) {
                Text(emptyLabel, style = MaterialTheme.typography.bodyMedium)
            } else {
                suggestions.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.product, fontWeight = FontWeight.Medium)
                            Text(
                                "${item.suggested_qty} units • ${item.confidence}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "${(item.score * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onAsk) {
                    Text("Ask Assistant")
                }
                Button(onClick = onOpen) {
                    Text("Review Run")
                }
            }
        }
    }
}

@Composable
private fun StoreInventoryCard(
    selectedStore: Int,
    storeOptions: List<Int>,
    storeStock: List<StockViewModel.StoreStockItem>,
    onStoreSelect: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Store $selectedStore snapshot", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                storeOptions.forEach { option ->
                    FilterChip(
                        selected = selectedStore == option,
                        onClick = { onStoreSelect(option) },
                        label = { Text(option.toString()) }
                    )
                }
            }

            if (storeStock.isEmpty()) {
                Text("No products found for this store.", style = MaterialTheme.typography.bodyMedium)
            } else {
                storeStock.take(5).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.product, fontWeight = FontWeight.Medium)
                            Text(
                                "${item.category ?: "General"} • ${item.uom ?: "pcs"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(item.quantity.toString(), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyConversationCard(onAskPrompt: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ask me like a teammate.", style = MaterialTheme.typography.titleMedium)
            Text(
                "I can explain workflows, suggest transfer items, check low stock, and help you find products.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Start with: What can you do?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onAskPrompt)
            )
        }
    }
}

@Composable
private fun MessageBubble(msg: AssistantViewModel.Message) {
    val isUser = msg.sender == "You"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (isUser) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(14.dp)
        ) {
            Text(msg.text, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun TypingDots() {
    var dots by remember { mutableStateOf(".") }

    LaunchedEffect(Unit) {
        while (true) {
            dots = when (dots) {
                "." -> ".."
                ".." -> "..."
                else -> "."
            }
            delay(350)
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Thinking$dots", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
