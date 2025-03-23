package maratische.telegram.pvddigest


import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.*

class SettingsUtil {

    companion object {
        var appProps = Properties()

        init {
            try {
                appProps.load(FileInputStream("pvdDigest.prop"))
            } catch (e: FileNotFoundException) {
                appProps.store(FileOutputStream("pvdDigest.prop"), null)
            }
        }

        fun save() {
            appProps.store(FileOutputStream("pvdDigest.prop"), null)
        }

        fun loadOffset(): Long = appProps.getProperty("telegram_offset", "0").toLong()
        fun saveOffset(offset: Long) {
            appProps.setProperty("telegram_offset", offset.toString())
        }
        fun loadDigestMessageId(): Long = appProps.getProperty("digest_message", "0").toLong()
        fun saveDigestMessageId(offset: Long) {
            appProps.setProperty("digest_message", offset.toString())
        }

        fun loadTelegramKey() = appProps.getProperty("telegram_key", "")
        fun loadTelegramKeySecondBot() = appProps.getProperty("telegram_key_second_bot", "")
        fun timeout(): Long = appProps.getProperty("timeoutInMS", "10000").toLong()
        fun destinationChannelId(): String = appProps.getProperty("destination_channel_id", "10000")
        fun sourceChatId(): String = appProps.getProperty("source_chat_id", "10000")
        fun publicSourceChatId(): String = appProps.getProperty("public_source_chat_id", "10000")

        //mode = digest - режим работы дайджестов
        //mode = snowtrips - режим работы для снежной тусовки, репостить сообщения
        //mode = baraholka - режим барахолка, пишешь заявку боту, проверка админом и подписка на каналы
        fun mode(): String = appProps.getProperty("mode", "digest")

    }
}