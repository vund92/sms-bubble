package com.smsbubble.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsbubble.app.ChatViewModel
import com.smsbubble.app.data.BubbleStyle
import com.smsbubble.app.data.Message

// ======================================================================
// Ghép tin nhắn thành danh sách hiển thị (nhãn thời gian + bong bóng)
// ======================================================================

private sealed interface ChatItem {
    val key: String

    data class Header(val label: String, val messageId: Long) : ChatItem {
        override val key: String get() = "h$messageId"
    }

    data class Bubble(val msg: Message, val first: Boolean, val last: Boolean) : ChatItem {
        override val key: String get() = "m${msg.id}"
    }
}

private const val HOUR = 60 * 60 * 1000L

/** Hiện nhãn thời gian khi là tin đầu tiên, sang ngày mới, hoặc cách tin trước hơn 1 giờ. */
private fun needsHeader(prev: Message?, cur: Message): Boolean =
    prev == null ||
        cur.timestamp - prev.timestamp > HOUR ||
        prev.timestamp.toLdt().toLocalDate() != cur.timestamp.toLdt().toLocalDate()

private fun buildChatItems(messages: List<Message>): List<ChatItem> {
    val out = ArrayList<ChatItem>(messages.size + 8)
    messages.forEachIndexed { i, m ->
        val prev = messages.getOrNull(i - 1)
        val next = messages.getOrNull(i + 1)
        val header = needsHeader(prev, m)
        if (header) out += ChatItem.Header(headerLabel(m.timestamp), m.id)
        val first = header || prev!!.outgoing != m.outgoing
        val last = next == null || next.outgoing != m.outgoing || needsHeader(m, next)
        out += ChatItem.Bubble(m, first, last)
    }
    return out
}

// ======================================================================
// Màn hình chat
// ======================================================================

@Composable
fun ChatScreen(vm: ChatViewModel, conversationId: Long, onBack: () -> Unit) {
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val allMessages by vm.messages.collectAsStateWithLifecycle()
    val style by vm.style.collectAsStateWithLifecycle()

    val conversation = conversations.firstOrNull { it.id == conversationId }
    if (conversation == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    BackHandler(onBack = onBack)

    val palette = bubblePalette(style)
    val name = rememberDisplayName(conversation)
    val otherShort = remember(name) { shortNameOf(name) }

    val messages = remember(allMessages, conversationId) {
        allMessages.filter { it.conversationId == conversationId }
            .sortedWith(compareBy<Message> { it.timestamp }.thenBy { it.id })
    }
    val chatItems = remember(messages) { buildChatItems(messages) }

    var editing by remember { mutableStateOf<Message?>(null) }
    var editContact by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    LaunchedEffect(chatItems.size) {
        if (chatItems.isNotEmpty()) listState.animateScrollToItem(chatItems.lastIndex)
    }

    val menuItems: @Composable (close: () -> Unit) -> Unit = { close ->
        DropdownMenuItem(
            text = { Text("Sửa tên / số điện thoại") },
            onClick = { close(); editContact = true },
        )
        DropdownMenuItem(
            text = { Text("Xoá cuộc trò chuyện") },
            onClick = { close(); confirmDelete = true },
        )
        HorizontalDivider()
        StyleMenuItems(style) { close(); vm.setStyle(it) }
    }

    Scaffold(
        containerColor = palette.chatBg,
        topBar = {
            if (style == BubbleStyle.IOS) {
                IosChatHeader(name, palette, onBack, onNameClick = { editContact = true }, menuItems)
            } else {
                AndroidChatTopBar(name, conversation.phone, onBack, menuItems)
            }
        },
        bottomBar = {
            Composer(
                style = style,
                palette = palette,
                otherShortName = otherShort,
                onSend = { text, outgoing, ts -> vm.addMessage(conversationId, text, outgoing, ts) },
            )
        },
    ) { padding ->
        if (chatItems.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Chưa có tin nhắn.\nChọn ai gửi, chọn thời gian, rồi nhập nội dung bên dưới.\n" +
                        "Nhấn giữ một bong bóng để sửa hoặc xoá.",
                    color = palette.meta,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
                items(chatItems, key = { it.key }) { item ->
                    when (item) {
                        is ChatItem.Header -> TimeHeader(item.label, palette)
                        is ChatItem.Bubble -> MessageBubble(
                            item = item,
                            style = style,
                            palette = palette,
                            onLongClick = { editing = item.msg },
                        )
                    }
                }
            }
        }
    }

    editing?.let { m ->
        EditMessageDialog(
            message = m,
            otherName = otherShort,
            onDismiss = { editing = null },
            onSave = {
                vm.updateMessage(it)
                editing = null
            },
            onDelete = {
                vm.deleteMessage(m.id)
                editing = null
            },
        )
    }

    if (editContact) {
        ContactDialog(
            initial = conversation,
            onDismiss = { editContact = false },
            onConfirm = { n, p ->
                vm.updateConversation(conversation.copy(name = n, phone = p))
                editContact = false
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Xoá cuộc trò chuyện?") },
            text = { Text("Toàn bộ tin nhắn với \"$name\" sẽ bị xoá.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteConversation(conversationId)
                }) { Text("Xoá", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Huỷ") } },
        )
    }
}

// ======================================================================
// Thanh tiêu đề
// ======================================================================

@Composable
private fun IosChatHeader(
    name: String,
    palette: BubblePalette,
    onBack: () -> Unit,
    onNameClick: () -> Unit,
    menuItems: @Composable (close: () -> Unit) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(color = palette.barBg) {
        Column(Modifier.statusBarsPadding()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(84.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Quay lại",
                        tint = palette.accent,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = 220.dp)
                        .clickable(onClick = onNameClick)
                        .padding(4.dp),
                ) {
                    Avatar(name, 48.dp, BubbleStyle.IOS)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            name,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            null,
                            tint = palette.meta,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Box(Modifier.align(Alignment.CenterEnd)) {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreHoriz, contentDescription = "Tuỳ chọn", tint = palette.accent)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        menuItems { menuOpen = false }
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = palette.meta.copy(alpha = 0.3f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AndroidChatTopBar(
    name: String,
    phone: String,
    onBack: () -> Unit,
    menuItems: @Composable (close: () -> Unit) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(name, 38.dp, BubbleStyle.ANDROID)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(name, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (phone.isNotBlank() && phone != name) {
                        Text(
                            phone,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        },
        actions = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Tuỳ chọn")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    menuItems { menuOpen = false }
                }
            }
        },
    )
}

// ======================================================================
// Nhãn thời gian + bong bóng
// ======================================================================

@Composable
private fun TimeHeader(label: String, palette: BubblePalette) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = palette.meta,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 6.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    item: ChatItem.Bubble,
    style: BubbleStyle,
    palette: BubblePalette,
    onLongClick: () -> Unit,
) {
    val m = item.msg
    val maxWidth = (LocalConfiguration.current.screenWidthDp * 0.75f).dp
    val ios = style == BubbleStyle.IOS

    val shape = if (ios) {
        IosBubbleShape(outgoing = m.outgoing, tail = item.last)
    } else {
        // Kiểu Google Messages: góc nhỏ ở chỗ các bong bóng liền nhau.
        val big = 20.dp
        val small = 4.dp
        val top = if (item.first) big else small
        val bottom = if (item.last) big else small
        if (m.outgoing) {
            RoundedCornerShape(topStart = big, topEnd = top, bottomEnd = bottom, bottomStart = big)
        } else {
            RoundedCornerShape(topStart = top, topEnd = big, bottomEnd = big, bottomStart = bottom)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = if (item.first) 6.dp else 2.dp),
        horizontalAlignment = if (m.outgoing) Alignment.End else Alignment.Start,
    ) {
        Box(
            Modifier
                // Chừa chỗ cho "đuôi" bong bóng kiểu iPhone.
                .padding(start = if (ios && !m.outgoing) 6.dp else 0.dp, end = if (ios && m.outgoing) 6.dp else 0.dp)
                .widthIn(max = maxWidth)
                .background(if (m.outgoing) palette.outBg else palette.inBg, shape)
                .combinedClickable(onClick = {}, onLongClick = onLongClick)
                .padding(horizontal = if (ios) 12.dp else 14.dp, vertical = if (ios) 7.dp else 10.dp)
        ) {
            Text(
                text = m.text,
                color = if (m.outgoing) palette.outText else palette.inText,
                fontSize = if (ios) 17.sp else 16.sp,
                lineHeight = if (ios) 22.sp else 21.sp,
            )
        }
        if (!ios && item.last) {
            Text(
                shortTime(m.timestamp),
                fontSize = 11.sp,
                color = palette.meta,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

// ======================================================================
// Ô soạn tin: chọn người gửi, chọn thời gian, nhập nội dung
// ======================================================================

@Composable
private fun Composer(
    style: BubbleStyle,
    palette: BubblePalette,
    otherShortName: String,
    onSend: (text: String, outgoing: Boolean, timestamp: Long) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var outgoing by rememberSaveable { mutableStateOf(true) }
    var customTime by rememberSaveable { mutableStateOf<Long?>(null) }
    var showTime by remember { mutableStateOf(false) }
    val ios = style == BubbleStyle.IOS

    fun send() {
        val t = text.trim()
        if (t.isEmpty()) return
        val ts = customTime ?: System.currentTimeMillis()
        onSend(t, outgoing, ts)
        text = ""
        // Nếu đang dùng thời gian tự chọn, tin tiếp theo tự lùi sau 1 phút để giữ đúng thứ tự.
        if (customTime != null) customTime = ts + 60_000L
    }

    Surface(color = palette.barBg) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                FilterChip(
                    selected = outgoing,
                    onClick = { outgoing = true },
                    label = { Text("Tôi gửi") },
                )
                FilterChip(
                    selected = !outgoing,
                    onClick = { outgoing = false },
                    label = { Text("$otherShortName gửi") },
                )
                AssistChip(
                    onClick = { showTime = true },
                    leadingIcon = {
                        Icon(Icons.Filled.Schedule, null, Modifier.size(AssistChipDefaults.IconSize))
                    },
                    label = {
                        Text(customTime?.let { fullLabel(it) } ?: "Thời gian: bây giờ")
                    },
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(if (outgoing) "Tin nhắn của bạn" else "Tin nhắn của $otherShortName")
                    },
                    shape = RoundedCornerShape(22.dp),
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.chatBg,
                        unfocusedContainerColor = palette.chatBg,
                        unfocusedBorderColor = palette.meta.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                val enabled = text.isNotBlank()
                if (ios) {
                    Box(
                        Modifier
                            .padding(bottom = 10.dp)
                            .size(34.dp)
                            .background(
                                if (enabled) palette.accent else palette.meta.copy(alpha = 0.4f),
                                CircleShape,
                            )
                            .clickable(enabled = enabled) { send() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.ArrowUpward, "Gửi", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                } else {
                    FilledIconButton(
                        onClick = { send() },
                        enabled = enabled,
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Gửi")
                    }
                }
            }
        }
    }

    if (showTime) {
        MessageTimeDialog(
            initial = customTime,
            allowNow = true,
            onDismiss = { showTime = false },
            onConfirm = {
                customTime = it
                showTime = false
            },
        )
    }
}
