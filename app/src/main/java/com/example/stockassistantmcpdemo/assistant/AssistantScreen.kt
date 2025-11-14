package com.example.stockassistantmcpdemo.assistant

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(vm: AssistantViewModel) {
    val fullScreen by vm.fullScreen.collectAsStateWithLifecycle()
    var showScanner by remember { mutableStateOf(false) }

    val speechLauncher = rememberSpeechRecognizer { spoken ->
        if (spoken.isNotBlank()) vm.send(spoken)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),

        // ✅ Add a TopAppBar
        topBar = {
            TopAppBar(
                title = { Text(if (fullScreen) "AI Assistant 🤖" else "Assistant Sheet") },
                actions = {
                    TextButton(onClick = { vm.toggleMode() }) {
                        Text(if (fullScreen) "Sheet" else "Full")
                    }
                }
            )
        },

        // ✅ Add the missing FAB
        floatingActionButton = {
            AssistantFAB {
                // Open speech recognizer on mic click
                speechLauncher()
            }
        }
    ) { pv ->
        val padded = PaddingValues(
            top = pv.calculateTopPadding(),
            bottom = pv.calculateBottomPadding() + 70.dp,
            start = pv.calculateStartPadding(LayoutDirection.Ltr),
            end = pv.calculateEndPadding(LayoutDirection.Ltr)
        )

        // ✅ Main body
        if (fullScreen) {
            FullScreenChat(
                vm = vm,
                paddingValues = padded,
                onOpenScanner = { showScanner = true }
            )
        } else {
            var open by remember { mutableStateOf(true) }
            if (open) {
                ModalBottomSheet(onDismissRequest = { open = false }) {
                    FullScreenChat(
                        vm = vm,
                        paddingValues = PaddingValues(12.dp),
                        onOpenScanner = { showScanner = true }
                    )
                }
            } else {
                Surface(Modifier.padding(padded)) {
                    Text("Tap the mic or toggle to open the assistant sheet.")
                }
            }
        }

        // ✅ Scanner overlay
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
