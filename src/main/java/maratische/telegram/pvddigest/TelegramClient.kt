package maratische.telegram.pvddigest

import com.google.gson.Gson
import maratische.telegram.pvddigest.SettingsUtil.Companion.loadOffset
import okhttp3.*
import okhttp3.internal.EMPTY_REQUEST
import java.io.IOException

class TelegramClient {
    private var client = OkHttpClient()
    var gson = Gson()
    private val baseUrl = "https://api.telegram.org/bot"
    private var secret = ""

    fun setSecret(secret: String) {
        this.secret = secret;
    }

    var process: ItemProcess? = null

    fun setProcessGetUpdatesItem(process: ItemProcess) {
        this.process = process
    }

    fun getAllTelegramUpdates() {
        try {
            val offset =
                1L + loadOffset()
            val request: Request = Request.Builder()
                .url("$baseUrl${secret}/getUpdates?offset=$offset")
                .post(EMPTY_REQUEST)
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    sendError("onFailure: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    var responseBody = response.body?.string()
                    val entity: GetUpdates =
                        gson.fromJson(responseBody, GetUpdates::class.java)
                    if (entity.ok && entity.result.isNotEmpty() ?: false) {
                        entity.result.sortedBy { it.update_id }.forEach {
                            process!!.process(it)
//                            processGetUpdatesItem(it)
                        }
                    }
                }

            })
        } catch (e: Exception) {
            e.message
            sendError("onFailure: ${e.message}")
        }
    }

    private fun sendError(message: String) {
        System.err.println(message)
    }

    fun processGetUpdatesItem(item: GetUpdatesItem) {
        System.out.println(item)
        if (item.message != null && item.message?.chat?.id == SettingsUtil.sourceChatId().toLong()) {
            var message = item.message?.text ?: ""
            if (message.lowercase().contains("#pvd")
                || message.lowercase().contains("#пвд")
            ) {
                if ((item.message?.chat?.title ?: null) == "pvd_test_chat") {
//                forwardMessage("pvd_test_channel", "pvd_test_chat", item.message?.message_id ?: 0L)
                    forwardMessage(
                        SettingsUtil.sourceChatId(),
                        SettingsUtil.destinationChannelId(),
                        item.message?.message_id ?: 0L
                    )
                }
            }
        }
        SettingsUtil.saveOffset(item.update_id)
        SettingsUtil.save()
    }

    fun forwardMessage(chatId: String, fromChatId: String, messageId: Long) {
        try {
            val multipartBody: MultipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM) // Header to show we are sending a Multipart Form Data
                .addFormDataPart(
                    "chat_id",
                    "$chatId"
                ) // other string params can be like userId, name or something
                .addFormDataPart("from_chat_id", "$fromChatId")
                .addFormDataPart("disable_notification", "true")
                .addFormDataPart(
                    "message_id",
                    "$messageId"
                ) // other string params can be like userId, name or something
//                .addFormDataPart("reply_markup", markup)
                .build()
            val request: Request = Request.Builder()
                .url("$baseUrl${secret}/forwardMessage")
                .post(multipartBody)
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    sendError("onFailure: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    System.out.println("onResponse -> ${response.body?.string()}")
                }
            })
        } catch (e: Exception) {
            sendError("onFailure: ${e.message}")
            e.message
        }
    }

    fun sendMessage(chatId: String, text: String) {
        try {
            val multipartBody: MultipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM) // Header to show we are sending a Multipart Form Data
                .addFormDataPart(
                    "chat_id",
                    "$chatId"
                ) // other string params can be like userId, name or something
                .addFormDataPart(
                    "text",
                    "$text"
                ) // other string params can be like userId, name or something
//                .addFormDataPart("reply_markup", markup)
                .build()
            val request: Request = Request.Builder()
                .url("$baseUrl${secret}/sendMessage")
                .post(multipartBody)
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    sendError("onFailure: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    System.out.println("onResponse -> ${response.body?.string()}")
                }
            })
        } catch (e: Exception) {
            sendError("onFailure: ${e.message}")
            e.message
        }
    }

}

interface ItemProcess {
    fun process(item: GetUpdatesItem);
}

data class GetUpdates(
    var ok: Boolean,
    var result: List<GetUpdatesItem>
)

data class GetUpdatesItem(
    var update_id: Long,
    var message: Message?,
    var edited_message: Message?,
    var channel_post: Message?
)

data class Message(
    var message_id: Long,
    var from: MessageUser?,
    var chat: MessageChat?,
    var date: Long,
    var text: String?
)

data class MessageUser(
    var id: Long,
    var is_bot: Boolean?,
    var first_name: String?,
    var username: String?,
    var language_code: String?
)

data class MessageChat(
    var id: Long,
    var title: String?,
    var first_name: String?,
    var username: String?,
    var language_code: String?,
    var type: String?
)