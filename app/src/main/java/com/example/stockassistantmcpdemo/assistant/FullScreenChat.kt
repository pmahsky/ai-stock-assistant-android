package com.example.stockassistantmcpdemo.assistant

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.ui.platform.LocalContext
import java.util.Locale


/**
 * A full-screen chat interface for interacting with the AI assistant.
 *
 * This composable displays the chat history, stock overview, store-specific inventory, and provides an input field
 * for sending messages. It also includes action chips for quick commands and a Text-to-Speech (TTS) feature to read
 * out the assistant's responses.
 *
 * @param vm The [AssistantViewModel] for managing the chat's state.
 * @param paddingValues The padding values to apply to the root Column.
 * @param onOpenScanner A callback to be invoked when the user clicks the scan action chip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenChat(
    vm: AssistantViewModel,
    paddingValues: PaddingValues,
    onOpenScanner: () -> Unit
) {
    val messages by vm.messages.collectAsState()
    val stockVM: StockViewModel = viewModel()
    val overview by stockVM.overview.collectAsState()
    val storeStock by stockVM.storeStock.collectAsState()

    var input by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedStore by remember { mutableIntStateOf(101) }
    val storeOptions = listOf(101, 102, 103)

    val isTyping by vm.isTyping.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // -----------------------------
    // 🎤 TEXT-TO-SPEECH STATE
    // -----------------------------
    var ttsEnabled by remember { mutableStateOf(true) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // Init TTS
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context, { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1f)
                Log.d("TTS_DEBUG", "TTS ready")
            }
        }, "com.google.android.tts")

    }


    // Shutdown TTS when screen removed
    DisposableEffect(Unit) {
        onDispose { tts?.shutdown() }
    }

    // ---------------------------------
    // 🗣️ SPEAK LAST AI MESSAGE
    // ---------------------------------
    LaunchedEffect(messages) {
        if (!ttsEnabled) {
            tts?.stop()
            return@LaunchedEffect
        }

        val last = messages.lastOrNull() ?: return@LaunchedEffect
        if (last.sender == "You") return@LaunchedEffect   // do not speak user msgs

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        tts?.speak(last.text, TextToSpeech.QUEUE_FLUSH, params, "AI_RESPONSE")
    }

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size, isTyping) {
        scope.launch { listState.animateScrollToItem(maxOf(messages.size - 1, 0)) }
    }

    // Initial data load
    LaunchedEffect(Unit) {
        stockVM.fetchOverview()
        stockVM.fetchStoreStock(selectedStore)
        stockVM.listenLiveUpdates()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Title
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "AI Assistant 🤖",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            IconButton(onClick = {
                ttsEnabled = !ttsEnabled
                if (!ttsEnabled) {
                    tts?.stop()     // stop ongoing speech
                }
            }) {
                Icon(
                    if (ttsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Toggle TTS"
                )
            }


        }
        Spacer(Modifier.height(6.dp))


        // Scrollable area
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Overview section
            item {
                overview?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("📦 Stock Overview", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Total Items: ${it.total_items}")
                            Text("Total Quantity: ${it.total_quantity}")
                            Text("Low Stock: ${it.low_stock}")
                            Text("Expiring Soon: ${it.expiring}")
                        }
                    }
                } ?: Text("Loading stock overview…")
            }

            // Action chips
            item {
                ActionChips(
                    onScan = onOpenScanner,
                    onLowStock = { vm.quickLowStock() },
                    onTransfer = { vm.quickTransfer() }
                )
            }

            // Store selection dropdown
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Store:", modifier = Modifier.padding(end = 8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        TextField(
                            value = selectedStore.toString(),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            storeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.toString()) },
                                    onClick = {
                                        selectedStore = option
                                        expanded = false
                                        scope.launch { stockVM.fetchStoreStock(option) }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Store inventory card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F4F4))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("🏬 Store $selectedStore Inventory", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        if (storeStock.isEmpty()) {
                            Text("No products found", style = MaterialTheme.typography.bodySmall)
                        } else {
                            storeStock.forEach { item ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(item.product, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${item.category ?: ""} (${item.uom ?: ""})",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Text("${item.quantity}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Chat messages
            items(messages) { msg ->
                val isUser = msg.sender == "You"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isUser) Color(0xFFDCF8C6) else Color(0xFFE5E5EA))
                            .padding(12.dp)
                    ) {
                        Text(msg.text, color = Color.Black)
                    }
                }
            }

            if (isTyping) {
                item { TypingDots() }
            }
        }

        // Input bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message…") },
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

/**
 * A row of action chips for quick commands.
 *
 * @param onScan A callback to be invoked when the user clicks the scan chip.
 * @param onLowStock A callback to be invoked when the user clicks the low stock chip.
 * @param onTransfer A callback to be invoked when the user clicks the transfer chip.
 */
@Composable
private fun ActionChips(
    onScan: () -> Unit,
    onLowStock: () -> Unit,
    onTransfer: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionChip(text = "📷 Scan", onClick = onScan)
        ActionChip(text = "📦 Low Stock", onClick = onLowStock)
        ActionChip(text = "🔁 Transfer", onClick = onTransfer)
    }
}

/**
 * A single action chip.
 *
 * @param text The text to display on the chip.
 * @param onClick A callback to be invoked when the user clicks the chip.
 */
@Composable
private fun ActionChip(text: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(50),
        modifier = Modifier.height(32.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * A composable that displays a typing indicator.
 */
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
                .background(Color(0xFFE5E5EA))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Typing$dots", color = Color.DarkGray)
        }
    }
}


