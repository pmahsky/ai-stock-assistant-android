package com.example.stockassistantmcpdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.example.stockassistantmcpdemo.assistant.AssistantScreen
import com.example.stockassistantmcpdemo.assistant.AssistantViewModel
import com.example.stockassistantmcpdemo.ui.theme.ChatViewModel

/**
 * The main activity of the application.
 *
 * This activity hosts the [AssistantScreen] composable, which provides the main UI of the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()
    /**
     * Called when the activity is first created.
     *
     * This method sets up the content of the activity by displaying the [AssistantScreen] composable.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm = AssistantViewModel()

            MaterialTheme {
                AssistantScreen(vm)
            }
        }
    }
}
