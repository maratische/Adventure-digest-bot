package maratische.telegram.pvddigest


import maratische.telegram.pvddigest.event.IncomingMessageEvent
import okhttp3.Response
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
open class TelegramService(
    private val eventPublisher: ApplicationEventPublisher
) {
    private var telegramClient: TelegramClient = TelegramClient()

    init {
        telegramClient.setSecret(SettingsUtil.loadTelegramKey())
        telegramClient.setProcessGetUpdatesItem(object : ItemProcess {
            override fun process(item: GetUpdatesItem) {
                val message = item.message ?: item.edited_message ?: return
                eventPublisher.publishEvent(IncomingMessageEvent(message))

                SettingsUtil.saveOffset(item.update_id)//TODO сохранять не при каждом сообщение а с какой то задержкой
                SettingsUtil.save()
            }
        })
    }

    fun sendMessage(
        chatId: String, text: String, markup: String = "",
        onResponse: ((acc: Response) -> Response)? = null
    ) {
        telegramClient.sendMessage(chatId, text, markup, onResponse)
    }

    fun editMessage(
        chatId: String, messageId: Long, text: String, markup: String = "",
        onResponse: ((acc: Response) -> Response)? = null
    ) = telegramClient.editMessage(chatId, messageId, text, markup, onResponse)

    fun deleteMessage(
        chatId: String, messageId: Long,
        onResponse: ((acc: Response) -> Response)? = null
    ) = telegramClient.deleteMessage(chatId, messageId, onResponse)


    fun forwardMessage(
        chatSourceId: String,
        chatDestinationId: String,
        messageId: Long,
        onResponse: ((acc: Response) -> Response)? = null
    ) = telegramClient.forwardMessage(chatSourceId, chatDestinationId, messageId, onResponse)

    @Scheduled(fixedDelay = 5000)
    fun scheduler() {
        telegramClient.getAllTelegramUpdates()
    }

}
