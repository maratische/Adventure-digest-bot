package maratische.telegram.pvddigest.listener

import maratische.telegram.pvddigest.TelegramService
import maratische.telegram.pvddigest.event.TelegramSendMessageEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
open class TelegramSendMessageListener(
    var telegramService: TelegramService,
) {

    @EventListener(TelegramSendMessageEvent::class)
    open fun processTelegramSendMessageEvent(event: TelegramSendMessageEvent) {
        logger.info(
            "send message to telegram {} {}", event.chatId,
            event.message?.substring(0, 100.coerceAtMost(event.message.length)) ?: ""
        )
        if (event.chatId != null && event.message != null) {
            telegramService.sendMessage(event.chatId.toString(), event.message)
        }
    }


    companion object {
        private val logger = LoggerFactory.getLogger(TelegramSendMessageListener::class.java)
    }
}