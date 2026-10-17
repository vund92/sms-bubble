package com.smsbubble.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.smsbubble.app.data.BubbleStyle
import com.smsbubble.app.data.ChatRepository
import com.smsbubble.app.data.Conversation
import com.smsbubble.app.data.Message

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ChatRepository(app)

    val conversations = repo.conversations
    val messages = repo.messages
    val style = repo.style

    fun setStyle(style: BubbleStyle) = repo.setStyle(style)

    fun addConversation(name: String, phone: String): Long = repo.addConversation(name, phone)
    fun updateConversation(c: Conversation) = repo.updateConversation(c)
    fun deleteConversation(id: Long) = repo.deleteConversation(id)

    fun addMessage(conversationId: Long, text: String, outgoing: Boolean, timestamp: Long) =
        repo.addMessage(conversationId, text, outgoing, timestamp)

    fun updateMessage(m: Message) = repo.updateMessage(m)
    fun deleteMessage(id: Long) = repo.deleteMessage(id)
}
