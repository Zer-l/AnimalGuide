package com.permissionx.animalguide.data.repository

import com.permissionx.animalguide.data.local.ChatConversationDao
import com.permissionx.animalguide.data.local.ChatMessageDao
import com.permissionx.animalguide.domain.model.ChatConversation
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val chatConversationDao: ChatConversationDao,
    private val chatMessageDao: ChatMessageDao
) {
    suspend fun getAll(): List<ChatConversation> =
        chatConversationDao.getAll().map {
            ChatConversation(
                id = it.id,
                title = it.title,
                type = it.type,
                createdAt = it.createdAt,
                lastMessageAt = it.lastMessageAt,
                previewText = it.previewText
            )
        }

    suspend fun delete(conversationId: String) {
        chatConversationDao.delete(conversationId)
        chatMessageDao.deleteAll(conversationId)
    }

    fun generateNewId(): String = UUID.randomUUID().toString()
}
