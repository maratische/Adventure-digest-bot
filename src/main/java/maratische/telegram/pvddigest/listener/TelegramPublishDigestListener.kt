package maratische.telegram.pvddigest.listener

import com.google.gson.Gson
import maratische.telegram.pvddigest.OnSendMessageReponse
import maratische.telegram.pvddigest.SettingsUtil
import maratische.telegram.pvddigest.TelegramService
import maratische.telegram.pvddigest.event.TelegramPublishDigestEvent
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
open class TelegramPublishDigestListener(
    private val telegramService: TelegramService
) {
    var gson = Gson()


    @EventListener(TelegramPublishDigestEvent::class)
    open fun processTelegramPublishDigestEvent(event: TelegramPublishDigestEvent) {
        var messageId = SettingsUtil.loadDigestMessageId()
        logger.info(
            "Publish digest messageId {} {}", messageId,
            event.message?.substring(0, 100.coerceAtMost(event.message.length)) ?: ""
        )
        if (event.message != null) {
            if (messageId > 0) {
                telegramService.editMessage(
                    SettingsUtil.sourceChatId(),
                    messageId,
                    event.message,
                    onResponse = { acc: Response ->
                        var responseBody = acc.body?.string()
                        logger.info(responseBody)
                        if (acc.code == 400) {
                            SettingsUtil.saveDigestMessageId(0);
                            SettingsUtil.save()
                        }
                        return@editMessage acc
                    })
            } else {
                telegramService.sendMessage(
                    SettingsUtil.sourceChatId(),
                    event.message,
                    onResponse = { acc: Response ->
                        if (acc.code == 200) {
                            var responseBody = acc.body?.string()
                            logger.info(responseBody)
                            val entity: OnSendMessageReponse =
                                gson.fromJson(responseBody, OnSendMessageReponse::class.java)
                            messageId = entity.result.message_id
                            logger.info("send New digest and return message_id $messageId")
                            SettingsUtil.saveDigestMessageId(messageId);
                            SettingsUtil.save()
                        }
                        return@sendMessage acc
                    })
            }
        }
    }


    companion object {
        private val logger = LoggerFactory.getLogger(TelegramPublishDigestListener::class.java)
    }

}