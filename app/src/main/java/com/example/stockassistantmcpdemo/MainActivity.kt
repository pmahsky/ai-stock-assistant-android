package com.example.stockassistantmcpdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.stockassistantmcpdemo.assistant.AssistantScreen
import com.example.stockassistantmcpdemo.assistant.AssistantViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.stockassistantmcpdemo.data.TransferAssistContext
import com.example.stockassistantmcpdemo.ui.TransferScreen
import com.example.stockassistantmcpdemo.ui.theme.StockAssistantMCPDemoTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm = remember { AssistantViewModel() }
            var currentScreen by remember { mutableStateOf("ASSISTANT") }
            var transferContext by remember { mutableStateOf<TransferAssistContext?>(null) }

            StockAssistantMCPDemoTheme {
                if (currentScreen == "TRANSFER") {
                    TransferScreen(
                        initialContext = transferContext,
                        onBack = {
                            currentScreen = "ASSISTANT"
                            transferContext = null
                        }
                    )
                } else {
                    AssistantScreen(
                        vm,
                        onOpenTransfer = { context ->
                            transferContext = context
                            currentScreen = "TRANSFER"
                        }
                    )
                }
            }
        }
    }
}
