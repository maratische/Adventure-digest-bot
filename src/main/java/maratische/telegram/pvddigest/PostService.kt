package maratische.telegram.pvddigest

import maratische.telegram.pvddigest.model.Post
import maratische.telegram.pvddigest.model.PostStatuses
import maratische.telegram.pvddigest.model.User
import maratische.telegram.pvddigest.model.UserRoles
import maratische.telegram.pvddigest.repository.PostRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class PostService(
    private val messageRepository: PostRepository
) {

    fun findByMessageId(messageId: Long): Post? = messageRepository.findByMessageId(messageId)

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
        var messageText = messageIn.text ?: ""
        var messageDb = findByMessageId(messageIn.message_id ?: 0L)
        if (messageText.lowercase().contains("#pvd")
            || messageText.lowercase().contains("#пвд")
        ) {
            if (messageDb == null) {//новое сообщение, сохраняем
                messageDb = Post()
                messageDb.messageId = messageDb.messageId
                messageDb.user = user
                messageDb.created = System.currentTimeMillis()
            }
            messageDb.content = messageText
            messageDb.date = parseDate(messageText)
            messageDb.updated = System.currentTimeMillis()
            if ((messageDb.date ?: 0) > 0) {//готов к модерации
                messageDb.status = if (user.role == UserRoles.BEGINNER) {
                    PostStatuses.MODERATING
                } else {
                    PostStatuses.PUBLISHED
                }
            } else {
                messageDb.status = PostStatuses.DRAFT
                //надо написать про ошибку
            }
            messageDb = save(messageDb)
        } else if (messageDb != null) {//сообщение такое есть и стало пустым
            messageDb.status = PostStatuses.DRAFT
            messageDb = save(messageDb)
        }
        return messageDb;
    }

    val dateRegex = Regex("#(\\d{4})-(\\d{1,2})-(\\d{1,2})")
    val dateTimeRegex = Regex("#(\\d{4})-(\\d{1,2})-(\\d{1,2})[TТ](\\d{1,2}):(\\d{1,2})")

    /**
     * парсим дату из сообщения
     * дата может быть #2024-11-29 или #2024-11-29Т18:00
     * причем Т может быть как русская так и английская
     */
    fun parseDate(message: String): Long {
        val matchTime = dateTimeRegex.find(message)
        if (matchTime != null) {
            var date = LocalDateTime.of(
                matchTime.groupValues[1].toInt(), matchTime.groupValues[2].toInt(),
                matchTime.groupValues[3].toInt(), matchTime.groupValues[4].toInt(), matchTime.groupValues[5].toInt()
            );
            return date.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
        }
        val match = dateRegex.find(message)
        if (match != null) {
            var date = LocalDateTime.of(
                match.groupValues[1].toInt(), match.groupValues[2].toInt(),
                match.groupValues[3].toInt(), 0, 0
            );
            return date.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
        }
        return 0;
    }

    fun save(message: Post) = messageRepository.save(message)
}