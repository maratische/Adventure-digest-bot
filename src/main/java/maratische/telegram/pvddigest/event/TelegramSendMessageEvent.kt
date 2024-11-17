package maratische.telegram.pvddigest.event

class TelegramSendMessageEvent(
    val chatId: Long?,
    val message: String?,
) {
}