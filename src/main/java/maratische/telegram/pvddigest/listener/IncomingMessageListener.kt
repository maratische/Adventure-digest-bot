package maratische.telegram.pvddigest.listener

import maratische.telegram.pvddigest.*
import maratische.telegram.pvddigest.event.IncomingMessageEvent
import maratische.telegram.pvddigest.event.PostEvent
import maratische.telegram.pvddigest.event.PublishDigestPostsEvent
import maratische.telegram.pvddigest.model.PostStatuses
import maratische.telegram.pvddigest.model.User
import maratische.telegram.pvddigest.model.UserRoles
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
open class IncomingMessageListener(
    private val userService: UserService,
    private val postService: PostService,
    private val telegramService: TelegramService,
    private val eventPublisher: ApplicationEventPublisher
) {
    @EventListener(IncomingMessageEvent::class)
    open fun processIncomingMessageEvent(incomingMessageEvent: IncomingMessageEvent) {
        try {
            val message = incomingMessageEvent.message
            val user = userService.getOtCreateUser(message.from)
            if (user.role == UserRoles.BANNED) {
                return
            }
            if (user != null && message.chat != null) {
                if (message.chat?.id == SettingsUtil.sourceChatId().toLong()) {
                    //работаем с нашей группой
                    postService.processMessage(message, user)
                } else
                    if (message.chat?.type == "private") {
                        //персональный чат
                        if (user.chatId == null) {
                            user.chatId = message.chat?.id
                            userService.save(user)
                        }
                        processPrivate(message, user)
                    } else {
                        logger.info("process post in unknown chat {} message {}", message.chat, message)
                    }
            }
        } catch (e: Exception) {
            logger.error("error on parse message $incomingMessageEvent")
        }
    }


    val userrole = Regex("/userrole[_-](\\w+)\\s(.*)")
    val confirm = Regex("/confirm[_-](\\d+)")
    val decline = Regex("/decline[_-](\\d+)")
    val closed = Regex("/closed[_-](\\d+)")

    //    @Transactional
    fun processPrivate(messageIn: Message, user: User) {
        if (SettingsUtil.mode() == "digest") {
            return processPrivateDigest(messageIn, user);
        } else
            if (SettingsUtil.mode() == "snowtrips") {
                return processPrivateSnowtrips(messageIn, user);
            } else
                if (SettingsUtil.mode() == "baraholka") {
                    return processPrivateBaraholka(messageIn, user);
                }
    }

    fun processPrivateBaraholka(messageIn: Message, user: User) {
    }

    fun processPrivateSnowtrips(messageIn: Message, user: User) {
    }

    fun processPrivateDigest(messageIn: Message, user: User) {
        if (messageIn.text?.lowercase() == "/help" || messageIn.text?.lowercase() == "help") {
            processPrivateHelp(user)
            return
        }
        if (messageIn.text?.lowercase() == "/users" && user.role == UserRoles.ADMIN) {
            processAdminUsersList(user)
            return
        }
        val matchUserrole = userrole.find(messageIn.text ?: "")
        if (matchUserrole != null && user.role == UserRoles.ADMIN) {
            processAdminSetRoleUser(user, matchUserrole.groupValues[1], matchUserrole.groupValues[2])
            processModeratorPrivateList(user)
            return
        }
        val matchClosed = closed.find(messageIn.text ?: "")
        if (matchClosed != null && (user.role == UserRoles.MODERATOR || user.role == UserRoles.ADMIN)) {
            processModeratorPrivateClosed(matchClosed, user)
            return
        }
        if (messageIn.text?.lowercase() == "/moder" && (user.role == UserRoles.MODERATOR || user.role == UserRoles.ADMIN)) {
            processModeratorPrivateModer(user)
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

    private fun processModeratorPrivateModer(user: User) {
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
            telegramService.sendMessage(
                user.chatId.toString(),
                " ${post.date} - ${user2?.username}\n" +
                        "${post.id}\n" +
                        "${post.content}",
                "{\"keyboard\":[[{\"text\":\"/moder\",\"hide\":false},{\"text\":\"/list\",\"hide\":false}]," + buttons + "]}"
            )
        }
    }

    private fun processModeratorPrivateClosed(matchClosed: MatchResult, user: User) {
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

    private fun processModeratorPrivateList(user: User) {
        //список постов на дайджесте
        val list = postService.findAllPublishedPosts()
        val buttons = "[{\"text\":\"/_1\",\"hide\":false}," +
                "{\"text\":\"/closed_1\",\"hide\":false}]" +
                "[{\"text\":\"/confirm_1\",\"hide\":false}," +
                "{\"text\":\"/decline_1\",\"hide\":false}]"
        list.forEach { post ->
            telegramService.sendMessage(
                user.chatId.toString(),
                "${post.messageId}\n" +
                        "${post.content}",
                "{\"keyboard\":[[{\"text\":\"/moder\",\"hide\":false},{\"text\":\"/list\",\"hide\":false}]," + buttons + "]}"
            )
        }
    }

    private fun processAdminUsersList(user: User) {
        val list = userService.list().sortedBy { it -> it.role }
        val message = list.map { it -> "${it.role?.name ?: ""} - ${it.telegramId} - ${it.username} - ${it.firstname}" }
            .joinToString(separator = "\n") + "\n" +
        "/userrole_BANNED 1\n" +
                "/userrole_BEGINNER 1\n" +
                "/userrole_TRAVELER 1\n" +
                "/userrole_ADVANCED 1\n" +
                "/userrole_MODERATOR 1\n" +
                "/userrole_ADMIN 1\n"
        telegramService.sendMessage(
            user.chatId.toString(),
            message
        )
    }

    private fun processAdminSetRoleUser(user: User, roleName: String, id: String) {
        try {
            val role = UserRoles.valueOf(roleName)
            val user2 = userService.findByTelegramId(id.toLong())
            if (user2 == null) {
                telegramService.sendMessage(
                    user.chatId.toString(),
                    "user $id не найдем"
                )
                return
            }
            user2.role = role
            userService.save(user2)
            val message = "$role to $user2"
            telegramService.sendMessage(
                user.chatId.toString(),
                message
            )
        } catch (e: Exception) {
            logger.error(e.message)
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
        if (user.role == UserRoles.ADMIN) {
            text += "/users - список пользователей\n"
        }
        telegramService.sendMessage(user.chatId.toString(), text)
    }


    companion object {
        private val logger = LoggerFactory.getLogger(IncomingMessageListener::class.java)
    }
}