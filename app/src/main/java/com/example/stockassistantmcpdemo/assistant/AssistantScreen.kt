package com.example.stockassistantmcpdemo.assistant

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stockassistantmcpdemo.data.TransferAssistContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(vm: AssistantViewModel, onOpenTransfer: (TransferAssistContext?) -> Unit) {
    val fullScreen by vm.fullScreen.collectAsStateWithLifecycle()
    val pendingTransferContext by vm.pendingTransferContext.collectAsStateWithLifecycle()
    var showScanner by remember { mutableStateOf(false) }

    val speechLauncher = rememberSpeechRecognizer { spoken ->
        if (spoken.isNotBlank()) vm.send(spoken)
    }

    LaunchedEffect(pendingTransferContext) {
        pendingTransferContext?.let { context ->
            onOpenTransfer(context)
            vm.consumeTransferContext()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (fullScreen) "Stock Assistant" else "Assistant Sheet") },
                actions = {
                    IconButton(onClick = { speechLauncher() }) {
                        Icon(Icons.Filled.Mic, contentDescription = "Voice input")
                    }
                    TextButton(onClick = { onOpenTransfer(null) }) {
                        Text("Transfer")
                    }
//                    TextButton(onClick = { vm.toggleMode() }) {
//                        Text(if (fullScreen) "Sheet" else "Full")
//                    }
                }
            )
        }
    ) { pv ->
        val padded = PaddingValues(
            top = pv.calculateTopPadding(),
            bottom = pv.calculateBottomPadding(),
            start = pv.calculateStartPadding(LayoutDirection.Ltr),
            end = pv.calculateEndPadding(LayoutDirection.Ltr)
        )

        if (fullScreen) {
            FullScreenChat(
                vm = vm,
                paddingValues = padded,
                onOpenScanner = { showScanner = true },
                onOpenTransfer = onOpenTransfer
            )
        } else {
            var open by remember { mutableStateOf(true) }
            if (open) {
                ModalBottomSheet(onDismissRequest = { open = false }) {
                    FullScreenChat(
                        vm = vm,
                        paddingValues = PaddingValues(12.dp),
                        onOpenScanner = { showScanner = true },
                        onOpenTransfer = onOpenTransfer
                    )
                }
            } else {
                Surface(Modifier.padding(padded)) {
                    Text("Tap the mic or toggle to open the assistant sheet.")
                }
            }
        }

        if (showScanner) {
            EnsureCameraPermission {
                BarcodeScannerScreen(
                    onScanResult = { code ->
                        vm.send("scan:$code")
                        showScanner = false
                    },
                    onClose = { showScanner = false }
                )
            }
        }
    }
}
