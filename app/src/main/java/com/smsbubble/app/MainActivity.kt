package com.smsbubble.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smsbubble.app.ui.AppTheme
import com.smsbubble.app.ui.ChatScreen
import com.smsbubble.app.ui.ConversationListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: ChatViewModel = viewModel()
            val style by vm.style.collectAsStateWithLifecycle()

            AppTheme(style) {
                var openId by rememberSaveable { mutableStateOf<Long?>(null) }
                val id = openId
                if (id == null) {
                    ConversationListScreen(vm = vm, onOpen = { openId = it })
                } else {
                    ChatScreen(vm = vm, conversationId = id, onBack = { openId = null })
                }
            }
        }
    }
}
