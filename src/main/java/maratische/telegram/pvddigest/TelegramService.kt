package maratische.telegram.pvddigest


import maratische.telegram.pvddigest.event.PostEvent
import maratische.telegram.pvddigest.event.PublishDigestPostsEvent
import maratische.telegram.pvddigest.event.TelegramPublishDigestEvent
import maratische.telegram.pvddigest.event.TelegramSendMessageEvent
import maratische.telegram.pvddigest.model.PostStatuses
import maratische.telegram.pvddigest.model.User
import maratische.telegram.pvddigest.model.UserRoles
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
open class TelegramService(
    private val userService: UserService,
    private val postService: PostService,
    private val eventPublisher: ApplicationEventPublisher
) {
    private var telegramClient: TelegramClient = TelegramClient()

    init {
        telegramClient.setSecret(SettingsUtil.loadTelegramKey())
        telegramClient.setProcessGetUpdatesItem(object : ItemProcess {
            override fun process(item: GetUpdatesItem) {
                val message = item.message ?: item.edited_message ?: return
                val user = userService.getOtCreateUser(message.from)
                if (user != null && message.chat != null) {
                    if (message.chat?.id == SettingsUtil.sourceChatId().toLong()) {
                        //работаем с нашей группой
                        postService.processMessage(message, user)
                    }
                    if (item.message?.chat?.type == "private") {
                        //персональный чат
                        if (user.chatId == null) {
                            user.chatId = item.message?.chat?.id
                            userService.save(user)
                        }
                        processPrivate(item.message!!, user)
                    }
                }
                SettingsUtil.saveOffset(item.update_id)
                SettingsUtil.save()
            }
        })
    }

    @EventListener(TelegramPublishDigestEvent::class)
    open fun processTelegramPublishDigestEvent(event: TelegramPublishDigestEvent) {
        logger.info(
            "Publish digest {}",
            event.message?.substring(0, 100.coerceAtMost(event.message.length)) ?: ""
        )
        SettingsUtil.sourceChatId().toLong()
        var messageId = SettingsUtil.loadDigestMessageId()
        if (event.message != null) {
            if (messageId > 0) {
                telegramClient.editMessage(
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
                telegramClient.sendMessage(
                    SettingsUtil.sourceChatId(),
                    event.message,
                    onResponse = { acc: Response ->
                        if (acc.code == 200) {
                            var responseBody = acc.body?.string()
                            logger.info(responseBody)
                            val entity: OnSendMessageReponse =
                                telegramClient.gson.fromJson(responseBody, OnSendMessageReponse::class.java)
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

    @EventListener(TelegramSendMessageEvent::class)
    open fun processTelegramSendMessageEvent(event: TelegramSendMessageEvent) {
        logger.info(
            "send message to telegram {} {}", event.chatId,
            event.message?.substring(0, 100.coerceAtMost(event.message.length)) ?: ""
        )
        if (event.chatId != null && event.message != null) {
            telegramClient.sendMessage(event.chatId.toString(), event.message)
        }
    }

    val confirm = Regex("/confirm_(\\d+)")
    val decline = Regex("/decline_(\\d+)")

    fun processPrivate(messageIn: maratische.telegram.pvddigest.Message, user: User) {
        if (messageIn.text?.lowercase() == "help") {
            telegramClient.sendMessage(user.chatId.toString(), "Привет. Я бот дайджеста")
            return
        }
        if (messageIn.text?.lowercase() == "/moder" && (user.role == UserRoles.MODERATOR || user.role == UserRoles.ADMIN)) {
            //список постов на модерацию
            var list = postService.findAllModeratingPosts()
            if (list.size > 5) {
                list = list.subList(0, 5)
            }
            var buttons = list.map { post ->
                "[{\"text\":\"/confirm_${post.id}\",\"hide\":false}," +
                        "{\"text\":\"/decline_${post.id}\",\"hide\":false}]"
            }.joinToString(separator = ",")
            list.forEach { post ->
                telegramClient.sendMessage(
                    user.chatId.toString(), " ${post.date} - ${post.user?.username}\n" +
                            "${post.id}\n" +
                            "${post.content}",
                    "{\"keyboard\":[" + buttons + "]}"
                )
            }
            return
        }
        if (messageIn.text?.lowercase() == "/digest" && user.role == UserRoles.ADMIN) {
            eventPublisher.publishEvent(PublishDigestPostsEvent())
        }
        val matchConfirm = confirm.find(messageIn.text ?: "")
        if (matchConfirm != null) {
            val id = matchConfirm.groupValues[1].toLong()
            val postOptional = postService.findById(id)
            if (postOptional.isPresent && postOptional.get().status == PostStatuses.MODERATING) {
                var post = postOptional.get()
                post.status = PostStatuses.PUBLISHED
                //пост опубликован
                post = postService.save(post)
                eventPublisher.publishEvent(PostEvent(post.id!!))
            }
            return
        }
        val matchDecline = decline.find(messageIn.text ?: "")
        if (matchDecline != null) {
            val id = matchDecline.groupValues[1].toLong()
            val postOptional = postService.findById(id)
            if (postOptional.isPresent && postOptional.get().status == PostStatuses.MODERATING) {
                val post = postOptional.get()
                post.status = PostStatuses.REJECTED
                //пост отклонен
                postService.save(post)
                eventPublisher.publishEvent(PostEvent(post.id!!, "Пост отклонен"))
            }
            return
        }
    }

    fun sendMessage(chatId: String, text: String, markup: String = "") {
        telegramClient.sendMessage(chatId, text, markup)
    }

    @Scheduled(fixedDelay = 5000)
    fun scheduler() {
        telegramClient.getAllTelegramUpdates()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TelegramService::class.java)
    }

}
