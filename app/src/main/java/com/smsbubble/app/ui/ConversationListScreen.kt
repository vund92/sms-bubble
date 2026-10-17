package com.smsbubble.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsbubble.app.ChatViewModel
import com.smsbubble.app.data.BubbleStyle
import com.smsbubble.app.data.Conversation
import com.smsbubble.app.data.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(vm: ChatViewModel, onOpen: (Long) -> Unit) {
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val messages by vm.messages.collectAsStateWithLifecycle()
    val style by vm.style.collectAsStateWithLifecycle()
    val palette = bubblePalette(style)

    var showNew by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Conversation?>(null) }

    // Mỗi cuộc trò chuyện kèm tin mới nhất, sắp xếp mới nhất lên đầu.
    val rows = remember(conversations, messages) {
        conversations
            .map { c -> c to messages.filter { it.conversationId == c.id }.maxByOrNull { it.timestamp } }
            .sortedByDescending { (c, last) -> last?.timestamp ?: c.id }
    }

    val styleMenu: @Composable () -> Unit = {
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.Palette, contentDescription = "Giao diện", tint = palette.accent)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                StyleMenuItems(style) {
                    vm.setStyle(it)
                    menuOpen = false
                }
            }
        }
    }

    Scaffold(
        containerColor = palette.chatBg,
        topBar = {
            if (style == BubbleStyle.IOS) {
                Column(
                    Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        styleMenu()
                        IconButton(onClick = { showNew = true }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Tin nhắn mới", tint = palette.accent)
                        }
                    }
                    Text(
                        "Tin nhắn",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                    )
                }
            } else {
                TopAppBar(
                    title = { Text("Tin nhắn") },
                    actions = { styleMenu() },
                )
            }
        },
        floatingActionButton = {
            if (style == BubbleStyle.ANDROID) {
                ExtendedFloatingActionButton(
                    onClick = { showNew = true },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Bắt đầu trò chuyện") },
                )
            }
        },
    ) { padding ->
        if (rows.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Chưa có cuộc trò chuyện nào.\nNhấn " +
                        (if (style == BubbleStyle.IOS) "biểu tượng bút ở góc trên" else "nút \"Bắt đầu trò chuyện\"") +
                        " để tạo mới.",
                    textAlign = TextAlign.Center,
                    color = palette.meta,
                )
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(rows, key = { it.first.id }) { (c, last) ->
                    ConversationRow(
                        conversation = c,
                        last = last,
                        style = style,
                        palette = palette,
                        onClick = { onOpen(c.id) },
                        onLongClick = { toDelete = c },
                    )
                }
            }
        }
    }

    if (showNew) {
        ContactDialog(
            initial = null,
            onDismiss = { showNew = false },
            onConfirm = { name, phone ->
                showNew = false
                onOpen(vm.addConversation(name, phone))
            },
        )
    }

    toDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Xoá cuộc trò chuyện?") },
            text = { Text("Toàn bộ tin nhắn với \"${c.name.ifBlank { c.phone }}\" sẽ bị xoá.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteConversation(c.id)
                    toDelete = null
                }) { Text("Xoá", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Huỷ") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    conversation: Conversation,
    last: Message?,
    style: BubbleStyle,
    palette: BubblePalette,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val name = rememberDisplayName(conversation)
    val ios = style == BubbleStyle.IOS
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = if (ios) 10.dp else 12.dp),
            verticalAlignment = if (ios) Alignment.Top else Alignment.CenterVertically,
        ) {
            Avatar(name, if (ios) 46.dp else 48.dp, style)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (ios) 17.sp else 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (last != null) {
                        Text(listLabel(last.timestamp), fontSize = 14.sp, color = palette.meta)
                    }
                    if (ios) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            null,
                            tint = palette.meta,
                        )
                    }
                }
                Text(
                    text = when {
                        last == null -> "Chưa có tin nhắn"
                        last.outgoing && !ios -> "Bạn: ${last.text}"
                        else -> last.text
                    },
                    color = palette.meta,
                    fontSize = if (ios) 15.sp else 14.sp,
                    maxLines = if (ios) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (ios) HorizontalDivider(Modifier.padding(start = 74.dp), thickness = 0.5.dp)
    }
}
