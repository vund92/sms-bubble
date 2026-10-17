package com.smsbubble.app.data

/** Kiểu giao diện bong bóng chat. */
enum class BubbleStyle { IOS, ANDROID }

/** Một cuộc trò chuyện với một liên hệ (tên + số điện thoại). */
data class Conversation(
    val id: Long,
    val name: String,
    val phone: String,
)

/**
 * Một tin nhắn.
 * @param outgoing true = "tôi" gửi (bong bóng bên phải), false = người kia gửi (bên trái).
 * @param timestamp thời điểm tin nhắn (epoch millis) — người dùng tự chọn hoặc nhập.
 */
data class Message(
    val id: Long,
    val conversationId: Long,
    val text: String,
    val outgoing: Boolean,
    val timestamp: Long,
)
