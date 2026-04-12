package com.permissionx.animalguide.data.repository

import com.permissionx.animalguide.BuildConfig
import com.permissionx.animalguide.data.local.ChatConversationDao
import com.permissionx.animalguide.data.local.ChatMessageDao
import com.permissionx.animalguide.data.local.entity.ChatConversationEntity
import com.permissionx.animalguide.data.local.entity.ChatMessageEntity
import com.permissionx.animalguide.data.remote.DoubaoApi
import com.permissionx.animalguide.data.remote.dto.DoubaoMessage
import com.permissionx.animalguide.data.remote.dto.DoubaoRequest
import com.permissionx.animalguide.domain.model.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimalChatRepository @Inject constructor(
    private val doubaoApi: DoubaoApi,
    private val chatMessageDao: ChatMessageDao,
    private val chatConversationDao: ChatConversationDao
) {
    companion object {
        private const val MAX_MESSAGES = 100
        const val TYPE_ANIMAL = "animal"
        const val TYPE_GENERAL = "general"

        fun animalSystemPrompt(animalName: String) =
            "你是一位专业的动物科普专家，现在正在与用户讨论「$animalName」。" +
            "请用简洁、准确、友好的语言回答用户关于该动物的问题。" +
            "如果用户提问与该动物无关，请礼貌地引导话题回到动物相关内容。"

        const val GENERAL_SYSTEM_PROMPT =
            "你是一位专业的动物科普专家，可以回答关于任何动物的问题，" +
            "包括习性、分布、保护状况、形态特征等内容。请用简洁、准确、友好的语言作答。"
    }

    suspend fun getHistory(conversationId: String): List<ChatMessage> =
        chatMessageDao.getMessages(conversationId).map { it.toChatMessage() }

    suspend fun sendMessage(
        conversationId: String,
        type: String,
        title: String,
        userText: String,
        systemPrompt: String
    ): Result<ChatMessage> {
        val now = System.currentTimeMillis()

        val userEntity = ChatMessageEntity(
            conversationId = conversationId,
            role = "user",
            content = userText,
            timestamp = now
        )
        chatMessageDao.insert(userEntity)

        val history = chatMessageDao.getMessages(conversationId)
        val messages = buildList {
            add(DoubaoMessage(role = "system", content = systemPrompt))
            addAll(history.map { DoubaoMessage(role = it.role, content = it.content) })
        }

        return runCatching {
            val response = doubaoApi.generateAnimalInfo(
                authorization = "Bearer ${BuildConfig.DOUBAO_API_KEY}",
                request = DoubaoRequest(model = BuildConfig.DOUBAO_ENDPOINT_ID, messages = messages)
            )
            val replyText = response.choices?.firstOrNull()?.message?.content
                ?: error("Empty response from Doubao API")

            val replyEntity = ChatMessageEntity(
                conversationId = conversationId,
                role = "assistant",
                content = replyText,
                timestamp = System.currentTimeMillis()
            )
            chatMessageDao.insert(replyEntity)

            val count = chatMessageDao.getCount(conversationId)
            if (count > MAX_MESSAGES) {
                chatMessageDao.deleteOldest(conversationId, count - MAX_MESSAGES)
            }

            // 更新/创建对话记录：标题只在首次创建时设置，后续保留已有标题
            val existing = chatConversationDao.getById(conversationId)
            chatConversationDao.upsert(
                ChatConversationEntity(
                    id = conversationId,
                    title = existing?.title ?: title,
                    type = type,
                    createdAt = existing?.createdAt ?: now,
                    lastMessageAt = replyEntity.timestamp,
                    previewText = replyText.take(40)
                )
            )

            replyEntity.toChatMessage()
        }
    }

    suspend fun clearHistory(conversationId: String) {
        chatMessageDao.deleteAll(conversationId)
        chatConversationDao.delete(conversationId)
    }

    private fun ChatMessageEntity.toChatMessage() = ChatMessage(
        id = id,
        role = role,
        content = content,
        timestamp = timestamp
    )
}
