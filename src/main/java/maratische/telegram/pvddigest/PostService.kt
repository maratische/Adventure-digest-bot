package maratische.telegram.pvddigest

import maratische.telegram.pvddigest.event.PostEvent
import maratische.telegram.pvddigest.model.Post
import maratische.telegram.pvddigest.model.PostStatuses
import maratische.telegram.pvddigest.model.User
import maratische.telegram.pvddigest.model.UserRoles
import maratische.telegram.pvddigest.repository.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Service
open class PostService(
    private val messageRepository: PostRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val userService: UserService
) {
    var formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    val dateRegex = Regex("#(\\d{4})-(\\d{1,2})-(\\d{1,2})")
    val dateTimeRegex = Regex("#(\\d{4})-(\\d{1,2})-(\\d{1,2})[TТ](\\d{1,2}):(\\d{1,2})")

    fun findByMessageId(messageId: Long): Post? = messageRepository.findByMessageId(messageId)

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
    open fun processMessage(messageIn: maratische.telegram.pvddigest.Message, user: User): Post? {
        logger.info("process post {} from {}", messageIn, user)
        val messageText = messageIn.text ?: ""
        var postDb = findByMessageId(messageIn.message_id ?: 0L)
        var message = ""
        if (messageText.lowercase().startsWith("/digest ") && messageIn.reply_to_message != null
            && (user.role == UserRoles.ADMIN || user.role == UserRoles.MODERATOR)
        ) {
            val date = parseDate(messageText)
            if (date > 0) {
                postDb = findByMessageId(messageIn.reply_to_message?.message_id ?: 0L) ?: Post()
                postDb.messageId = messageIn.reply_to_message?.message_id
                postDb.userId = user.id
                postDb.created = System.currentTimeMillis()
                postDb.status = PostStatuses.PUBLISHED
                postDb.content = messageIn.reply_to_message?.text
                postDb.date = parseDate(messageText)
                postDb.updated = System.currentTimeMillis()
                postDb = save(postDb)
            }
        } else
        if (messageText.lowercase().contains("#pvd")
            || messageText.lowercase().contains("#пвд")
        ) {
            if (postDb == null) {//новое сообщение, сохраняем
                postDb = Post()
                postDb.messageId = messageIn.message_id
                postDb.userId = user.id
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
        postDb?.let {
            eventPublisher.publishEvent(PostEvent(postDb.id, message))
        }
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