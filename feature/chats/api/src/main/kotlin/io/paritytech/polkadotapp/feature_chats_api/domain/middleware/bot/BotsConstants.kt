package io.paritytech.polkadotapp.feature_chats_api.domain.middleware.bot

import io.paritytech.polkadotapp.common.utils.FeatureOption
import io.paritytech.polkadotapp.common.utils.isEnabled
import io.paritytech.polkadotapp.feature_chats_api.domain.model.ChatExtensionId
import io.paritytech.polkadotapp.feature_chats_api.domain.model.ChatId

class ChatBotData private constructor(
    val id: ChatExtensionId,
    val name: String
) {
    companion object {
        fun sample() = ChatBotData(id = "SampleBot", name = "Sample")
        fun weeklyGame() = ChatBotData(id = "WeeklyGameBot", name = "Polkadot Prizes")
        fun defaultBots() = buildList {
            if (FeatureOption.SAMPLE_BOT.isEnabled) {
                add(sample())
            }
        }

        fun disabledBots() = listOf(weeklyGame())
    }

    val chatId: ChatId
        get() = ChatId.fromChatBotId(id)
}
