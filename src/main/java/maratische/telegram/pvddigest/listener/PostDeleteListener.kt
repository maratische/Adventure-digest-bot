package maratische.telegram.pvddigest.listener

import maratische.telegram.pvddigest.TelegramService
import maratische.telegram.pvddigest.event.PostDeleteEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
open class PostDeleteListener(
    var telegramService: TelegramService
) {

    @EventListener(PostDeleteEvent::class)
    open fun processPostDeleteEvent(postEvent: PostDeleteEvent) {
        telegramService.deleteMessage(postEvent.chatId.toString(), postEvent.messageId)

    }
}