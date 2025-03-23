package maratische.telegram.pvddigest.listener

import maratische.telegram.pvddigest.SettingsUtil
import maratische.telegram.pvddigest.TelegramService
import maratische.telegram.pvddigest.event.PostForwardEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class PostForwardListener(
    var telegramService: TelegramService
) {

    @EventListener(PostForwardEvent::class)
    fun processPostDeleteEvent(postEvent: PostForwardEvent) {
        telegramService.forwardMessage(
            postEvent.chatId.toString(),
            SettingsUtil.destinationChannelId(),
            postEvent.messageId
        )
    }
}