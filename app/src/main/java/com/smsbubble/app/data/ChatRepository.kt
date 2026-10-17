package com.smsbubble.app.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

/**
 * Lưu toàn bộ dữ liệu trong một file JSON nội bộ của app (filesDir/chats.json).
 * Dữ liệu nhỏ nên đọc một lần khi khởi động, ghi lại ở luồng nền mỗi khi thay đổi.
 */
class ChatRepository(context: Context) {

    private val file = File(context.filesDir, "chats.json")
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val io = Executors.newSingleThreadExecutor()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _style = MutableStateFlow(
        runCatching { BubbleStyle.valueOf(prefs.getString(KEY_STYLE, null) ?: "") }
            .getOrDefault(BubbleStyle.IOS)
    )
    val style: StateFlow<BubbleStyle> = _style.asStateFlow()

    private var lastId = 0L

    init {
        load()
    }

    // ---------- Cài đặt ----------

    fun setStyle(style: BubbleStyle) {
        prefs.edit().putString(KEY_STYLE, style.name).apply()
        _style.value = style
    }

    // ---------- Cuộc trò chuyện ----------

    @Synchronized
    fun addConversation(name: String, phone: String): Long {
        val id = nextId()
        _conversations.value = _conversations.value + Conversation(id, name, phone)
        save()
        return id
    }

    @Synchronized
    fun updateConversation(conversation: Conversation) {
        _conversations.value = _conversations.value.map {
            if (it.id == conversation.id) conversation else it
        }
        save()
    }

    @Synchronized
    fun deleteConversation(id: Long) {
        _conversations.value = _conversations.value.filterNot { it.id == id }
        _messages.value = _messages.value.filterNot { it.conversationId == id }
        save()
    }

    // ---------- Tin nhắn ----------

    @Synchronized
    fun addMessage(conversationId: Long, text: String, outgoing: Boolean, timestamp: Long) {
        _messages.value = _messages.value +
            Message(nextId(), conversationId, text, outgoing, timestamp)
        save()
    }

    @Synchronized
    fun updateMessage(message: Message) {
        _messages.value = _messages.value.map { if (it.id == message.id) message else it }
        save()
    }

    @Synchronized
    fun deleteMessage(id: Long) {
        _messages.value = _messages.value.filterNot { it.id == id }
        save()
    }

    // ---------- Lưu / đọc file ----------

    private fun nextId(): Long {
        lastId = maxOf(System.currentTimeMillis(), lastId + 1)
        return lastId
    }

    private fun save() {
        val root = JSONObject()
        root.put("conversations", JSONArray().apply {
            _conversations.value.forEach { c ->
                put(
                    JSONObject()
                        .put("id", c.id)
                        .put("name", c.name)
                        .put("phone", c.phone)
                )
            }
        })
        root.put("messages", JSONArray().apply {
            _messages.value.forEach { m ->
                put(
                    JSONObject()
                        .put("id", m.id)
                        .put("conversationId", m.conversationId)
                        .put("text", m.text)
                        .put("outgoing", m.outgoing)
                        .put("timestamp", m.timestamp)
                )
            }
        })
        val json = root.toString()
        io.execute {
            try {
                val tmp = File(file.parentFile, "chats.json.tmp")
                tmp.writeText(json)
                if (!tmp.renameTo(file)) {
                    file.writeText(json)
                    tmp.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Không ghi được dữ liệu", e)
            }
        }
    }

    private fun load() {
        if (!file.exists()) return
        try {
            val root = JSONObject(file.readText())
            val convs = mutableListOf<Conversation>()
            root.optJSONArray("conversations")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    convs += Conversation(
                        id = o.getLong("id"),
                        name = o.optString("name"),
                        phone = o.optString("phone"),
                    )
                }
            }
            val msgs = mutableListOf<Message>()
            root.optJSONArray("messages")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    msgs += Message(
                        id = o.getLong("id"),
                        conversationId = o.getLong("conversationId"),
                        text = o.optString("text"),
                        outgoing = o.optBoolean("outgoing", true),
                        timestamp = o.getLong("timestamp"),
                    )
                }
            }
            _conversations.value = convs
            _messages.value = msgs
            lastId = maxOf(
                convs.maxOfOrNull { it.id } ?: 0L,
                msgs.maxOfOrNull { it.id } ?: 0L,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Không đọc được dữ liệu", e)
        }
    }

    private companion object {
        const val TAG = "ChatRepository"
        const val KEY_STYLE = "bubble_style"
    }
}
