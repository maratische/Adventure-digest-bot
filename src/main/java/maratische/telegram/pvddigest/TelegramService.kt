package maratische.telegram.pvddigest


import maratische.telegram.pvddigest.event.PostEvent
import maratische.telegram.pvddigest.event.PublishDigestPostsEvent
import maratische.telegram.pvddigest.model.PostStatuses
import maratische.telegram.pvddigest.model.User
import maratische.telegram.pvddigest.model.UserRoles
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

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
                    } else
                    if (item.message?.chat?.type == "private") {
                        //персональный чат
                        if (user.chatId == null) {
                            user.chatId = item.message?.chat?.id
                            userService.save(user)
                        }
                        processPrivate(item.message!!, user)
                    } else {
                        logger.info("process post in unknown chat {} message {}", item.message?.chat, message)
                    }
                }
                SettingsUtil.saveOffset(item.update_id)
                SettingsUtil.save()
            }
        })
    }

    val confirm = Regex("/confirm_(\\d+)")
    val decline = Regex("/decline_(\\d+)")
    val closed = Regex("/closed[_-](\\d+)")

    //    @Transactional
    open fun processPrivate(messageIn: Message, user: User) {
        if (messageIn.text?.lowercase() == "/help" || messageIn.text?.lowercase() == "help") {
            processPrivateHelp(user)
            return
        }
        if (messageIn.text?.lowercase() == "/list" && (user.role == UserRoles.MODERATOR || user.role == UserRoles.ADMIN)) {
            processPrivateList(user)
            return
        }
        val matchClosed = closed.find(messageIn.text ?: "")
        if (matchClosed != null && (user.role == UserRoles.MODERATOR || user.role == UserRoles.ADMIN)) {
            processPrivateClosed(matchClosed, user)
            return
        }
        if (messageIn.text?.lowercase() == "/moder" && (user.role == UserRoles.MODERATOR || user.role == UserRoles.ADMIN)) {
            processPrivateModer(user)
            return
        }
        if (messageIn.text?.lowercase() == "/digest" && user.role == UserRoles.ADMIN) {
            eventPublisher.publishEvent(PublishDigestPostsEvent())
            return
        }
        val matchConfirm = confirm.find(messageIn.text ?: "")
        if (matchConfirm != null && (user.role == UserRoles.MODERATOR || user.role == UserRoles.ADMIN)) {
            processPrivateConfirm(matchConfirm)
            return
        }
        val matchDecline = decline.find(messageIn.text ?: "")
        if (matchDecline != null && (user.role == UserRoles.MODERATOR || user.role == UserRoles.ADMIN)) {
            processPrivateDecline(matchDecline)
            return
        }
    }

    private fun processPrivateDecline(matchDecline: MatchResult) {
        val id = matchDecline.groupValues[1].toLong()
        val postOptional = postService.findById(id)
        if (postOptional.isPresent && postOptional.get().status == PostStatuses.MODERATING) {
            val post = postOptional.get()
            post.status = PostStatuses.REJECTED
            //пост отклонен
            postService.save(post)
            eventPublisher.publishEvent(PostEvent(post.id!!, "Пост отклонен"))
        }
    }

    private fun processPrivateConfirm(matchConfirm: MatchResult) {
        val id = matchConfirm.groupValues[1].toLong()
        val postOptional = postService.findById(id)
        if (postOptional.isPresent && postOptional.get().status == PostStatuses.MODERATING) {
            var post = postOptional.get()
            post.status = PostStatuses.PUBLISHED
            //пост опубликован
            post = postService.save(post)
            eventPublisher.publishEvent(PostEvent(post.id!!))
        }
    }

    private fun processPrivateModer(user: User) {
        //список постов на модерацию
        var list = postService.findAllModeratingPosts()
        if (list.size > 5) {
            list = list.subList(0, 5)
        }
        val buttons = list.map { post ->
            "[{\"text\":\"/confirm_${post.id}\",\"hide\":false}," +
                    "{\"text\":\"/decline_${post.id}\",\"hide\":false}]"
        }.joinToString(separator = ",")
        list.forEach { post ->
            val user2 = userService.findById(post.userId).getOrNull()
            telegramClient.sendMessage(
                user.chatId.toString(),
                " ${post.date} - ${user2?.username}\n" +
                        "${post.id}\n" +
                        "${post.content}",
                "{\"keyboard\":[[{\"text\":\"/moder\",\"hide\":false},{\"text\":\"/list\",\"hide\":false}]," + buttons + "]}"
            )
        }
    }

    private fun processPrivateClosed(matchClosed: MatchResult, user: User) {
        val id = matchClosed.groupValues[1].toLong()
        val postOptional = postService.findByMessageId(id)
        if (postOptional != null) {
            postOptional.status = PostStatuses.CLOSED
            postService.save(postOptional)
            logger.info("Пост закрыт {} модератором {}", postOptional, user)
            eventPublisher.publishEvent(PostEvent(postOptional.id!!, "Пост отклонен"))
            eventPublisher.publishEvent(PublishDigestPostsEvent())
        }
    }

    private fun processPrivateList(user: User) {
        //список постов на дайджесте
        val list = postService.findAllPublishedPosts()
        val buttons = "[{\"text\":\"/_1\",\"hide\":false}," +
                "{\"text\":\"/closed_1\",\"hide\":false}]"
        list.forEach { post ->
            telegramClient.sendMessage(
                user.chatId.toString(),
                "${post.messageId}\n" +
                        "${post.content}",
                "{\"keyboard\":[[{\"text\":\"/moder\",\"hide\":false},{\"text\":\"/list\",\"hide\":false}]," + buttons + "]}"
            )
        }
    }

    private fun processPrivateHelp(user: User) {
        var text = "Привет. Я бот дайджеста.\n" +
                "список команд\n"
        text += "/help - помощь, список доступных команд\n"
        if ((user.role == UserRoles.MODERATOR || user.role == UserRoles.ADMIN)) {
            text += "/list - список постов на дайджесте\n"
            text += "/moder - список на подерацию\n"
            text += "/digest - перегенерирует дайджест\n"
            text += "/digest #2024-12-12 - ответ на какое то сообщение, добавляет его в дайджест\n"
        }
        telegramClient.sendMessage(user.chatId.toString(), text)
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

    @Scheduled(fixedDelay = 5000)
    fun scheduler() {
        telegramClient.getAllTelegramUpdates()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TelegramService::class.java)
    }

}
