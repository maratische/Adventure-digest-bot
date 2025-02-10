package maratische.telegram.pvddigest

import maratische.telegram.pvddigest.event.PostDeleteEvent
import maratische.telegram.pvddigest.event.PostEvent
import maratische.telegram.pvddigest.model.Post
import maratische.telegram.pvddigest.model.PostStatuses
import maratische.telegram.pvddigest.model.User
import maratische.telegram.pvddigest.model.UserRoles
import maratische.telegram.pvddigest.repository.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs


@Service
open class PostService(
    private val messageRepository: PostRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val userService: UserService,
    private val postRepository: PostRepository
) {
    var formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    val dateRegex = Regex("#(\\d{4})-(\\d{1,2})-(\\d{1,2})")
    val dateTimeRegex = Regex("#(\\d{4})-(\\d{1,2})-(\\d{1,2})[TТ](\\d{1,2}):(\\d{1,2})")

    fun findByMessageId(messageId: Long): Mono<Post> = messageRepository.findByMessageId(messageId)

    fun findById(messageId: Long) = messageRepository.findById(messageId)

    fun findAllModeratingPosts() = messageRepository.findByStatus(PostStatuses.MODERATING)

    fun findAllPublishedPosts() = messageRepository.findByStatus(PostStatuses.PUBLISHED)

    fun short200(content: String?) = content?.substring(0, 200.coerceAtMost(content.length)) ?: ""

    /**
     * приходит сообщение,
     * если новое
     * - то мы создаем его с статусом DRAFT если чего то не хватает
     * - если всего хватает - создаем со статусом MODERATING
     * если уже имеющееся
     * - DRAFT, MODERATING, PUBLISHED и всего хватает - MODERATING
     * - DRAFT, MODERATING, PUBLISHED и чего то не  хватает - DRAFT (в будущем переделать на версии и роли)
     *
     * /digest #2024-12-12
     * если сообщение является ответом на  другое и автор - админ-модератор, добавляем его в дайджест
     */
//    @Transactional
    open fun processMessage(messageIn: Message, user: User): Mono<Post?> {
        logger.info("process post {} from {}", messageIn, user)
        val messageText = messageIn.text ?: ""
        var postDb = findByMessageId(messageIn.message_id).block()
        if (messageText.lowercase().startsWith("/digest ") && messageIn.reply_to_message != null
            && (user.role == UserRoles.ADMIN || user.role == UserRoles.MODERATOR)
        ) {
            return processMessageDigest(messageText, postDb, messageIn, user).switchIfEmpty(Mono.justOrEmpty(null))
        } else
            if (messageText.lowercase().contains("#pvd")
                || messageText.lowercase().contains("#пвд")
            ) {
                return processMessagePvd(postDb, messageIn, user, messageText).switchIfEmpty(Mono.justOrEmpty(null))
            } else if (postDb != null) {//сообщение такое есть и стало пустым
                postDb.status = PostStatuses.DRAFT
                eventPublisher.publishEvent(PostEvent(postDb.id, "Пост переведен в черновики"))
                return save(postDb)
            }
        return Mono.just(postDb)
    }

    private fun processMessagePvd(
        postDb1: Post?,
        messageIn: Message,
        user: User,
        messageText: String
    ): Mono<Post> {
        val postDb = postDb1 ?: Post(messageIn.message_id, user.id)
        postDb.content = messageText
        postDb.date = parseDate(messageText)
        postDb.updated = System.currentTimeMillis()
        if ((postDb.date ?: 0) > 0) {//готов к модерации
            postDb.status = if (user.role == UserRoles.BEGINNER) {
                PostStatuses.MODERATING
            } else if (user.role == UserRoles.TRAVELER) {
                val posts = postRepository.findByUserAndStatus(user.id, PostStatuses.PUBLISHED)
                    .filter { abs(it.date!! - postDb.date!!) < 1000 * 60 * 60 * 24 }.toStream().toList()
                if (posts.isNotEmpty()) {
                    PostStatuses.MODERATING
                } else {
                    PostStatuses.PUBLISHED
                }
            } else if (user.role == UserRoles.ADVANCED || user.role == UserRoles.MODERATOR || user.role == UserRoles.ADMIN) {
                PostStatuses.PUBLISHED
            } else {
                PostStatuses.REJECTED
            }
            eventPublisher.publishEvent(PostEvent(postDb.id, messageText))
        } else {
            postDb.status = PostStatuses.DRAFT
            //надо написать про ошибку
            eventPublisher.publishEvent(
                PostEvent(
                    postDb.id,
                    "Дата должна быть в формате #2024-11-29 или #2024-11-29Т18:00. Пост переведен в черновики." +
                            " Обновите его пожалуйста"
                )
            )
        }
        return save(postDb)
    }

    private fun processMessageDigest(
        messageText: String,
        postDb: Post,
        messageIn: Message,
        user: User
    ): Mono<Post> {
        var postDb1 = postDb
        val date = parseDate(messageText)
        if (date > 0) {
            postDb1 = findByMessageId(messageIn.reply_to_message?.message_id ?: 0L).block() ?: Post()
            postDb1.messageId = messageIn.reply_to_message?.message_id
            val userReply = userService.getOtCreateUser(messageIn.reply_to_message?.from).block()
            postDb1.userId = userReply?.id ?: user.id
            postDb1.created = System.currentTimeMillis()
            postDb1.status = PostStatuses.PUBLISHED
            postDb1.content = messageIn.reply_to_message?.text
            postDb1.date = parseDate(messageText)
            postDb1.updated = System.currentTimeMillis()
            eventPublisher.publishEvent(PostDeleteEvent(messageIn.chat?.id!!, messageIn.message_id))
            eventPublisher.publishEvent(PostEvent(postDb.id, messageText))
            //                telegramService.deleteMessage(messageIn.chat?.id.toString(), messageIn.message_id)
            return save(postDb1)
        }
        return Mono.just(postDb1)
    }

    /**
     * парсим дату из сообщения
     * дата может быть #2024-11-29 или #2024-11-29Т18:00
     * причем Т может быть как русская так и английская
     */
    fun parseDate(message: String): Long {
        val matchTime = dateTimeRegex.find(message)
        if (matchTime != null) {
            val date = LocalDateTime.of(
                matchTime.groupValues[1].toInt(), matchTime.groupValues[2].toInt(),
                matchTime.groupValues[3].toInt(), matchTime.groupValues[4].toInt(), matchTime.groupValues[5].toInt()
            )
            return date.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000
        }
        val match = dateRegex.find(message)
        if (match != null) {
            val date = LocalDateTime.of(
                match.groupValues[1].toInt(), match.groupValues[2].toInt(),
                match.groupValues[3].toInt(), 0, 0
            )
            return date.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000
        }
        return 0
    }

    fun save(message: Post) = messageRepository.save(message)

    companion object {
        private val logger = LoggerFactory.getLogger(PostService::class.java)
    }
}