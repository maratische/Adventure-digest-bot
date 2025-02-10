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
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.concurrent.Executors
import kotlin.jvm.optionals.getOrNull

@Service
open class IncomingMessageListener(
    private val userService: UserService,
    private val postService: PostService,
    private val telegramService: TelegramService,
    private val eventPublisher: ApplicationEventPublisher,
    private val r2dbcEntityTemplate: R2dbcEntityTemplate
) {

    val virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()
    val virtualScheduler = Schedulers.fromExecutor(virtualThreadExecutor)

    @EventListener(IncomingMessageEvent::class)
    open fun processIncomingMessageEvent(incomingMessageEvent: IncomingMessageEvent) {
        try {
            val message = incomingMessageEvent.message
            userService.getOtCreateUser(message.from).flatMap { user ->
                if (user.role == UserRoles.BANNED) {
                    return@flatMap Mono.empty<User>()
                } else {
                    return@flatMap Mono.just(user)
                }
            }
//                .subscribeOn(virtualScheduler)
//                .publishOn(Schedulers.boundedElastic())
                .flatMap { user ->
                    if (user != null && message.chat != null) {
                        if (message.chat?.id == SettingsUtil.sourceChatId().toLong()) {
                            //работаем с нашей группой
                            return@flatMap postService.processMessage(message, user)
                        } else
                            if (message.chat?.type == "private") {
                                //персональный чат
                                if (user.chatId == null) {
                                    user.chatId = message.chat?.id
                                    userService.save(user).subscribe()
                                }
                                return@flatMap processPrivate(message, user)
                            } else {
                                logger.info("process post in unknown chat {} message {}", message.chat, message)
                            }
                    }
                    return@flatMap Mono.empty<User>()
                }.subscribe()
        } catch (e: Exception) {
            logger.error("error on parse message $incomingMessageEvent", e)
        }
    }


    val userrole = Regex("/userrole[_-](\\w+)\\s(.*)")
    val confirm = Regex("/confirm[_-](\\d+)")
    val decline = Regex("/decline[_-](\\d+)")
    val closed = Regex("/closed[_-](\\d+)")

    //    @Transactional
    fun processPrivate(messageIn: Message, user: User): Mono<User> {
        if (messageIn.text?.lowercase() == "/admin") {
            return processAdmin(user)
        }
        if (messageIn.text?.lowercase() == "/help" || messageIn.text?.lowercase() == "help") {
            processPrivateHelp(user)
            return Mono.just(user)
        }
        if (messageIn.text?.lowercase() == "/users" && user.role == UserRoles.ADMIN) {
            return processAdminUsersList(user)
        }
        val matchUserrole = userrole.find(messageIn.text ?: "")
        if (matchUserrole != null && user.role == UserRoles.ADMIN) {
            processAdminSetRoleUser(user, matchUserrole.groupValues[1], matchUserrole.groupValues[2])
            processModeratorPrivateList(user)
            return Mono.empty()
        }
        val matchClosed = closed.find(messageIn.text ?: "")
        if (matchClosed != null && (user.role == UserRoles.MODERATOR || user.role == UserRoles.ADMIN)) {
            processModeratorPrivateClosed(matchClosed, user)
            return Mono.empty()
        }
        if (messageIn.text?.lowercase() == "/moder" && (user.role == UserRoles.MODERATOR || user.role == UserRoles.ADMIN)) {
            processModeratorPrivateModer(user)
            return Mono.empty()
        }
        if (messageIn.text?.lowercase() == "/digest" && user.role == UserRoles.ADMIN) {
            eventPublisher.publishEvent(PublishDigestPostsEvent())
            return Mono.empty()
        }
        val matchConfirm = confirm.find(messageIn.text ?: "")
        if (matchConfirm != null && (user.role == UserRoles.MODERATOR || user.role == UserRoles.ADMIN)) {
            processPrivateConfirm(matchConfirm)
            return Mono.empty()
        }
        val matchDecline = decline.find(messageIn.text ?: "")
        if (matchDecline != null && (user.role == UserRoles.MODERATOR || user.role == UserRoles.ADMIN)) {
            return processPrivateDecline(matchDecline)
        }
        return Mono.empty()
    }

    private fun processPrivateDecline(matchDecline: MatchResult): Mono<User> {
        val id = matchDecline.groupValues[1].toLong()
        val postOptional = postService.findById(id).blockOptional()
        if (postOptional.isPresent && postOptional.get().status == PostStatuses.MODERATING) {
            val post = postOptional.get()
            post.status = PostStatuses.REJECTED
            //пост отклонен
            eventPublisher.publishEvent(PostEvent(post.id!!, "Пост отклонен"))
            postService.save(post).subscribe()
        }
        return Mono.empty()
    }

    private fun processPrivateConfirm(matchConfirm: MatchResult) {
        val id = matchConfirm.groupValues[1].toLong()
        val postOptional = postService.findById(id).blockOptional()
        if (postOptional.isPresent && postOptional.get().status == PostStatuses.MODERATING) {
            var post = postOptional.get()
            post.status = PostStatuses.PUBLISHED
            //пост опубликован
            post = postService.save(post).block()!!
            eventPublisher.publishEvent(PostEvent(post.id!!))
        }
    }

    private fun processModeratorPrivateModer(user: User) {
        //список постов на модерацию
        var list = postService.findAllModeratingPosts().toStream().toList()
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
        val postOptional = postService.findByMessageId(id).block()
        if (postOptional != null) {
            postOptional.status = PostStatuses.CLOSED
            postService.save(postOptional).subscribe()
            logger.info("Пост закрыт {} модератором {}", postOptional, user)
            eventPublisher.publishEvent(PostEvent(postOptional.id!!, "Пост отклонен"))
            eventPublisher.publishEvent(PublishDigestPostsEvent())
        }
    }

    private fun processModeratorPrivateList(user: User) {
        //список постов на дайджесте
        val list = postService.findAllPublishedPosts().toStream().toList()
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

    private fun processAdminUsersList(user: User): Mono<User> {
        return userService.list().collectSortedList(compareBy<User>(User::role)).map { list ->
            list.joinToString(separator = "\n")
            { it -> "${it.role?.name ?: ""} - ${it.telegramId} - ${it.username} - ${it.firstname}" } + "\n" +
                    "/userrole_BANNED 1\n" +
                    "/userrole_BEGINNER 1\n" +
                    "/userrole_TRAVELER 1\n" +
                    "/userrole_ADVANCED 1\n" +
                    "/userrole_MODERATOR 1\n" +
                    "/userrole_ADMIN 1\n"
        }.map { message: String ->
            var unit: Unit = telegramService.sendMessage(
                user.chatId.toString(),
                message
            )
            user
        }

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
            userService.save(user2).subscribe()
            val message = "$role to $user2"
            telegramService.sendMessage(
                user.chatId.toString(),
                message
            )
        } catch (e: Exception) {
            logger.error(e.message)
        }
    }

    private fun processAdmin(user: User): Mono<User> {
        return userService.processAdmin()
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