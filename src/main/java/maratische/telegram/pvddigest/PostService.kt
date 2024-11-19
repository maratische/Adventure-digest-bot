package maratische.telegram.pvddigest

import maratische.telegram.pvddigest.event.PostEvent
import maratische.telegram.pvddigest.event.PublishDigestPostsEvent
import maratische.telegram.pvddigest.event.TelegramPublishDigestEvent
import maratische.telegram.pvddigest.event.TelegramSendMessageEvent
import maratische.telegram.pvddigest.model.Post
import maratische.telegram.pvddigest.model.PostStatuses
import maratische.telegram.pvddigest.model.User
import maratische.telegram.pvddigest.model.UserRoles
import maratische.telegram.pvddigest.repository.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter



@Service
open class PostService(
    private val messageRepository: PostRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    var formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    val dateRegex = Regex("#(\\d{4})-(\\d{1,2})-(\\d{1,2})")
    val dateTimeRegex = Regex("#(\\d{4})-(\\d{1,2})-(\\d{1,2})[TТ](\\d{1,2}):(\\d{1,2})")

    fun findByMessageId(messageId: Long): Post? = messageRepository.findByMessageId(messageId)

    fun findById(messageId: Long) = messageRepository.findById(messageId)

    fun findAllModeratingPosts() = messageRepository.findByStatus(PostStatuses.MODERATING)

    //сгенерировать пост дайджест
    @EventListener(PublishDigestPostsEvent::class)
    open fun publishPostsEvent(publishPostsEvent: PublishDigestPostsEvent) {
        val posts = messageRepository.findByStatus(PostStatuses.PUBLISHED).sortedBy { it.date }
        val mainPost = posts.map { post ->
            var content = (post.content ?: "").replace(dateTimeRegex, " ").replace(dateRegex, "")
            content = content.substring(0, 200.coerceAtMost(content.length))
            "${
                formatter.format(
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(post.date ?: 0),
                        ZoneId.systemDefault()
                    )
                )
            }\n" +
                    "${content}\n" +
                    "https://t.me/c/${SettingsUtil.publicSourceChatId()}/${post.messageId}"
        }.joinToString(separator = "\n\n")
        eventPublisher.publishEvent(TelegramPublishDigestEvent("Дайджест ПВД (от ${formatter.format(LocalDateTime.now())})\n\n $mainPost"))
    }

    @EventListener(PostEvent::class)
    open fun processPostEvent(postEvent: PostEvent) {
        logger.info("process post {}", postEvent)
        val postOptional = messageRepository.findById(postEvent.postId ?: return@processPostEvent)
        if (postOptional.isPresent) {
            val post = postOptional.get()
            when (post.status) {
                PostStatuses.DRAFT, PostStatuses.REJECTED -> {
                    eventPublisher.publishEvent(TelegramSendMessageEvent(post.user?.chatId, postEvent.message))
                }

                PostStatuses.MODERATING -> {
                    eventPublisher.publishEvent(
                        TelegramSendMessageEvent(
                            post.user?.chatId,
                            "Пост будет добавлен после одобрения модератором"
                        )
                    )
                    //написать модераторам
                }

                PostStatuses.PUBLISHED -> {
                    eventPublisher.publishEvent(TelegramSendMessageEvent(post.user?.chatId, "Пост опубликован"))
                    eventPublisher.publishEvent(PublishDigestPostsEvent())
                }

                PostStatuses.CLOSED -> {
                    eventPublisher.publishEvent(TelegramSendMessageEvent(post.user?.chatId, "Пост закрыт"))
                    //написать модераторам
                }

                null -> {
                    logger.info("empty status")
                }
            }
        }
    }

    /**
     * приходит сообщение,
     * если новое
     * - то мы создаем его с статусом DRAFT если чего то не хватает
     * - если всего хватает - создаем со статусом MODERATING
     * если уже имеющееся
     * - DRAFT, MODERATING, PUBLISHED и всего хватает - MODERATING
     * - DRAFT, MODERATING, PUBLISHED и чего то не  хватает - DRAFT (в будущем переделать на версии и роли)
     */
    fun processMessage(messageIn: maratische.telegram.pvddigest.Message, user: User): Post? {
        val messageText = messageIn.text ?: ""
        var postDb = findByMessageId(messageIn.message_id ?: 0L)
        var message = ""
        if (messageText.lowercase().contains("#pvd")
            || messageText.lowercase().contains("#пвд")
        ) {
            if (postDb == null) {//новое сообщение, сохраняем
                postDb = Post()
                postDb.messageId = messageIn.message_id
                postDb.user = user
                postDb.created = System.currentTimeMillis()
            }
            postDb.content = messageText
            postDb.date = parseDate(messageText)
            postDb.updated = System.currentTimeMillis()
            if ((postDb.date ?: 0) > 0) {//готов к модерации
                postDb.status = if (user.role == UserRoles.BEGINNER) {
                    PostStatuses.MODERATING
                } else {
                    PostStatuses.PUBLISHED
                }
            } else {
                postDb.status = PostStatuses.DRAFT
                //надо написать про ошибку
                message = "Дата должна быть в формате #2024-11-29 или #2024-11-29Т18:00. Пост переведен в черновики." +
                        " Обновите его пожалуйста"
            }
            postDb = save(postDb)
        } else if (postDb != null) {//сообщение такое есть и стало пустым
            postDb.status = PostStatuses.DRAFT
            postDb = save(postDb)
        }
        eventPublisher.publishEvent(PostEvent(postDb?.id, message))
        return postDb
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