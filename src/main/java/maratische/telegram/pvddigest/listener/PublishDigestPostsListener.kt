package maratische.telegram.pvddigest.listener

import maratische.telegram.pvddigest.PostService
import maratische.telegram.pvddigest.SettingsUtil
import maratische.telegram.pvddigest.UserService
import maratische.telegram.pvddigest.event.PostEvent
import maratische.telegram.pvddigest.event.PublishDigestPostsEvent
import maratische.telegram.pvddigest.event.TelegramPublishDigestEvent
import maratische.telegram.pvddigest.model.PostStatuses
import maratische.telegram.pvddigest.repository.PostRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.stream.Collectors
import kotlin.jvm.optionals.getOrNull

@Service
open class PublishDigestPostsListener(
    private val messageRepository: PostRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private var postService: PostService,
    private val userService: UserService,
) {

    //сгенерировать пост дайджест
//    @Transactional
    @EventListener(PublishDigestPostsEvent::class)
    open fun publishPostsEvent(publishPostsEvent: PublishDigestPostsEvent) {
        val posts = messageRepository.findByStatus(PostStatuses.PUBLISHED).toStream().collect(Collectors.toList())
            .sortedBy { it.date }
        posts.filter { (it.date ?: 0) < System.currentTimeMillis() }.forEach {
            it.status = PostStatuses.CLOSED
            postService.save(it).subscribe()
            eventPublisher.publishEvent(PostEvent(it.id))
        }
        val mainPost = posts.filter { (it.date ?: 0) >= System.currentTimeMillis() }.map { post ->
            var content =
                (post.content ?: "").replace(postService.dateTimeRegex, " ").replace(postService.dateRegex, "")
            content = postService.short200(content)
            val user = userService.findById(post.userId).getOrNull()
            "${
                postService.formatter.format(
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(post.date ?: 0),
                        ZoneId.systemDefault()
                    )
                )
            }\n" +
                    "${content}\n" +
                    "@${user?.username}\n" +
                    "https://t.me/c/${SettingsUtil.publicSourceChatId()}/${post.messageId}"
        }.joinToString(separator = "\n\n")
        eventPublisher.publishEvent(
            TelegramPublishDigestEvent(
                "Дайджест ПВД (от ${postService.formatter.format(LocalDateTime.now())})\n\n $mainPost \n\n" +
                        "Для попадания в дайджест сообщение должно содержать тег #пвд, дату в формате #2024-11-29 или #2024-11-29Т18:00\n" +
                        "и его должен одобрить модератор"
            )
        )
    }

}