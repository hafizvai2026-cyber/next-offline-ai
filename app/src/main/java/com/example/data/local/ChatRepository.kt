package com.example.data.local

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    val allConversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()

    fun searchConversations(query: String): Flow<List<ConversationEntity>> =
        conversationDao.searchConversations(query)

    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForConversation(conversationId)

    suspend fun getConversation(id: String): ConversationEntity? =
        conversationDao.getConversationById(id)

    suspend fun createNewConversation(
        title: String = "নতুন কথোপকথন",
        initialInstruction: String? = null
    ): ConversationEntity {
        val newConv = ConversationEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            messageCount = 0,
            lastSnippet = "",
            systemInstruction = initialInstruction
        )
        conversationDao.insertOrUpdate(newConv)
        return newConv
    }

    suspend fun saveUserMessage(
        conversationId: String,
        content: String
    ): MessageEntity {
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "user",
            content = content,
            timestamp = System.currentTimeMillis(),
            tokenCount = estimateTokens(content)
        )
        messageDao.insertMessage(message)
        updateConversationMetadata(conversationId, content)
        return message
    }

    suspend fun saveAssistantMessage(
        conversationId: String,
        content: String,
        modelName: String,
        isError: Boolean = false
    ): MessageEntity {
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "assistant",
            content = content,
            timestamp = System.currentTimeMillis(),
            tokenCount = estimateTokens(content),
            isError = isError,
            modelName = modelName
        )
        messageDao.insertMessage(message)
        updateConversationMetadata(conversationId, content)
        return message
    }

    private suspend fun updateConversationMetadata(conversationId: String, lastContent: String) {
        val count = messageDao.getMessageCount(conversationId)
        val snippet = if (lastContent.length > 80) lastContent.take(80) + "..." else lastContent
        val conv = conversationDao.getConversationById(conversationId)

        // If default title, auto-generate title from first user prompt
        val newTitle = if (conv != null && (conv.title == "নতুন কথোপকথন" || conv.title == "New Conversation")) {
            generateSimpleTitle(lastContent)
        } else {
            conv?.title ?: "কথোপকথন"
        }

        conversationDao.insertOrUpdate(
            ConversationEntity(
                id = conversationId,
                title = newTitle,
                createdAt = conv?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                messageCount = count,
                lastSnippet = snippet,
                systemInstruction = conv?.systemInstruction
            )
        )
    }

    suspend fun renameConversation(conversationId: String, newTitle: String) {
        conversationDao.renameConversation(conversationId, newTitle)
    }

    suspend fun deleteConversation(conversationId: String) {
        messageDao.deleteMessagesForConversation(conversationId)
        conversationDao.deleteConversation(conversationId)
    }

    suspend fun clearAllHistory() {
        messageDao.deleteAllMessages()
        conversationDao.deleteAllConversations()
    }

    suspend fun getRecentContext(conversationId: String, limit: Int = 10): List<MessageEntity> {
        return messageDao.getRecentMessages(conversationId, limit).reversed()
    }

    private fun generateSimpleTitle(prompt: String): String {
        val clean = prompt.trim()
            .replace("\n", " ")
            .take(35)
        return if (clean.isBlank()) "কথোপকথন" else clean
    }

    private fun estimateTokens(text: String): Int {
        return (text.length / 3).coerceAtLeast(1)
    }
}
