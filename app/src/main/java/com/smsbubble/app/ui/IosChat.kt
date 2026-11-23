package com.smsbubble.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smsbubble.app.data.BubbleStyle
import com.smsbubble.app.data.Message

/** Màu "kính" của nút nổi / ô nhập kiểu iPhone. */
@Composable
private fun glassColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color.White

@Composable
private fun iosText(): Color =
    if (isSystemInDarkTheme()) Color.White else Color.Black

@Composable
private fun iosLink(): Color =
    if (isSystemInDarkTheme()) IosLinkDark else IosLinkLight

// ======================================================================
// Bố cục màn hình chat kiểu iPhone
// ======================================================================

@Composable
internal fun IosChatLayout(
    name: String,
    palette: BubblePalette,
    chatItems: List<ChatItem>,
    listState: LazyListState,
    otherShortName: String,
    onBack: () -> Unit,
    onNameClick: () -> Unit,
    menuItems: @Composable (close: () -> Unit) -> Unit,
    onLongClick: (Message) -> Unit,
    onSend: (text: String, outgoing: Boolean, timestamp: Long) -> Unit,
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerSpace = topInset + 128.dp

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.chatBg)
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (chatItems.isEmpty()) {
                Text(
                    "Chưa có tin nhắn.\nBấm + để chọn người gửi và ngày giờ, rồi nhập nội dung.\n" +
                        "Nhấn giữ một bong bóng để sửa hoặc xoá.",
                    color = palette.meta,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 14.dp, end = 14.dp, top = headerSpace, bottom = 10.dp
                    ),
                ) {
                    items(chatItems, key = { it.key }) { item ->
                        when (item) {
                            is ChatItem.Header -> IosTimeHeader(item.label, palette)
                            is ChatItem.Bubble -> IosBubble(item, palette) { onLongClick(item.msg) }
                        }
                    }
                }
            }
            IosFloatingHeader(name, palette, onBack, onNameClick, menuItems)
        }
        IosComposer(palette, otherShortName, onSend)
    }
}

// ======================================================================
// Header nổi: nút quay lại tròn, avatar ở giữa, tên trong viên thuốc
// ======================================================================

@Composable
private fun GlassCircle(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = glassColor(),
        shadowElevation = 5.dp,
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun IosFloatingHeader(
    name: String,
    palette: BubblePalette,
    onBack: () -> Unit,
    onNameClick: () -> Unit,
    menuItems: @Composable (close: () -> Unit) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val bg = palette.chatBg
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to bg,
                    0.65f to bg.copy(alpha = 0.88f),
                    1f to bg.copy(alpha = 0f),
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 14.dp)
            .padding(top = 4.dp, bottom = 18.dp)
    ) {
        GlassCircle(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 8.dp),
            onClick = onBack,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Quay lại",
                tint = iosText(),
                modifier = Modifier.size(34.dp),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNameClick,
                ),
        ) {
            Avatar(name, 66.dp, BubbleStyle.IOS)
            Surface(
                shape = RoundedCornerShape(50),
                color = glassColor(),
                shadowElevation = 4.dp,
                modifier = Modifier.offset(y = (-12).dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                ) {
                    Text(
                        name,
                        color = iosText(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 200.dp),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        null,
                        tint = palette.meta,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp)
        ) {
            GlassCircle(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = "Tuỳ chọn", tint = iosText())
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                menuItems { menuOpen = false }
            }
        }
    }
}

// ======================================================================
// Nhãn thời gian: giờ chữ thường, ngày chữ đậm — "11:39 Thứ 6, 13 thg 11"
// ======================================================================

@Composable
private fun IosTimeHeader(label: String, palette: BubblePalette) {
    val cut = label.indexOf(' ')
    Text(
        text = buildAnnotatedString {
            if (cut > 0) {
                withStyle(SpanStyle(fontWeight = FontWeight.Normal)) { append(label.substring(0, cut + 1)) }
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(label.substring(cut + 1)) }
            } else {
                append(label)
            }
        },
        fontSize = 13.sp,
        color = palette.meta,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 8.dp),
    )
}

// ======================================================================
// Bong bóng
// ======================================================================

private val URL_RE = Regex("""(?i)\b(?:https?://|www\.)[^\s]*[^\s.,;:!?)"']""")
private val MONEY_RE = Regex(
    """\b(?:USD|AED|EUR|GBP|CNY|JPY|VND|SGD|AUD|CAD|KRW|THB|HKD|INR|CHF|RUB)\s?\d[\d,]*(?:\.\d+)?"""
)

/** Gạch chân link (màu xanh) và số tiền (cùng màu chữ) giống iPhone tự nhận diện. */
private fun styledMessage(text: String, linkColor: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    val links = URL_RE.findAll(text).map { it.range }.toList()
    links.forEach {
        addStyle(
            SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
            it.first, it.last + 1,
        )
    }
    MONEY_RE.findAll(text).forEach { m ->
        if (links.none { m.range.first in it }) {
            addStyle(SpanStyle(textDecoration = TextDecoration.Underline), m.range.first, m.range.last + 1)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IosBubble(item: ChatItem.Bubble, palette: BubblePalette, onLongClick: () -> Unit) {
    val m = item.msg
    val maxWidth = (LocalConfiguration.current.screenWidthDp * 0.78f).dp
    val textColor = if (m.outgoing) palette.outText else palette.inText
    val linkColor = if (m.outgoing) palette.outText else iosLink()
    val styled = remember(m.text, linkColor) { styledMessage(m.text, linkColor) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = if (item.first) 3.dp else 2.dp, bottom = if (item.last) 3.dp else 0.dp),
        horizontalAlignment = if (m.outgoing) Alignment.End else Alignment.Start,
    ) {
        Box(
            Modifier
                .padding(start = if (m.outgoing) 0.dp else 6.dp, end = if (m.outgoing) 6.dp else 0.dp)
                .widthIn(max = maxWidth)
                .background(
                    if (m.outgoing) palette.outBg else palette.inBg,
                    IosBubbleShape(outgoing = m.outgoing, tail = item.last, radius = 20.dp),
                )
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = onLongClick,
                )
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text(styled, color = textColor, fontSize = 17.sp, lineHeight = 23.sp)
        }
    }
}

// ======================================================================
// Ô soạn tin: nút + tròn (chọn người gửi / ngày giờ) và ô "Tin nhắn văn bản • SMS"
// ======================================================================

@Composable
private fun IosComposer(
    palette: BubblePalette,
    otherShortName: String,
    onSend: (text: String, outgoing: Boolean, timestamp: Long) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var outgoing by rememberSaveable { mutableStateOf(true) }
    var customTime by rememberSaveable { mutableStateOf<Long?>(null) }
    var showTime by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    fun send() {
        val t = text.trim()
        if (t.isEmpty()) return
        val ts = customTime ?: System.currentTimeMillis()
        onSend(t, outgoing, ts)
        text = ""
        if (customTime != null) customTime = ts + 60_000L
    }

    // Mặc định giống hệt iPhone; khi đổi người gửi / giờ thì gợi ý hiện thông tin đó.
    val placeholder = if (outgoing && customTime == null) {
        "Tin nhắn văn bản • SMS"
    } else {
        (if (outgoing) "Tôi" else otherShortName) + " • " +
            (customTime?.let { headerLabel(it) } ?: "bây giờ")
    }

    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.chatBg)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Box {
            GlassCircle(size = 48.dp, onClick = { menuOpen = true }) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Người gửi và thời gian",
                    tint = iosText(),
                    modifier = Modifier.size(28.dp),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                Text(
                    "Người gửi",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.meta,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                DropdownMenuItem(
                    text = { Text("Tôi (bên phải)") },
                    onClick = { outgoing = true; menuOpen = false },
                    trailingIcon = { if (outgoing) Icon(Icons.Filled.Check, null) },
                )
                DropdownMenuItem(
                    text = { Text("$otherShortName (bên trái)") },
                    onClick = { outgoing = false; menuOpen = false },
                    trailingIcon = { if (!outgoing) Icon(Icons.Filled.Check, null) },
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Chọn ngày giờ…") },
                    leadingIcon = { Icon(Icons.Filled.Schedule, null) },
                    onClick = { menuOpen = false; showTime = true },
                )
                if (customTime != null) {
                    DropdownMenuItem(
                        text = { Text("Dùng giờ hiện tại") },
                        onClick = { customTime = null; menuOpen = false },
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = glassColor(),
            shadowElevation = 5.dp,
            modifier = Modifier.weight(1f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .padding(start = 18.dp, end = 7.dp),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .padding(vertical = 11.dp)
                ) {
                    if (text.isEmpty()) {
                        Text(
                            placeholder,
                            color = palette.meta,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = TextStyle(color = iosText(), fontSize = 17.sp, lineHeight = 22.sp),
                        cursorBrush = SolidColor(palette.accent),
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (text.isBlank()) {
                    Icon(
                        Icons.Outlined.Mic,
                        contentDescription = null,
                        tint = palette.meta,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(24.dp),
                    )
                } else {
                    Box(
                        Modifier
                            .size(34.dp)
                            .background(palette.outBg, CircleShape)
                            .clickable { send() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.ArrowUpward,
                            contentDescription = "Gửi",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
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
