package maratische.telegram.pvddigest.listener

import maratische.telegram.pvddigest.PostService
import maratische.telegram.pvddigest.UserService
import maratische.telegram.pvddigest.event.PostEvent
import maratische.telegram.pvddigest.event.PublishDigestPostsEvent
import maratische.telegram.pvddigest.event.TelegramSendMessageEvent
import maratische.telegram.pvddigest.model.PostStatuses
import maratische.telegram.pvddigest.repository.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
open class PostListener(
    private val messageRepository: PostRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val postService: PostService,
    private val userService: UserService,
) {


    @EventListener(PostEvent::class)
    open fun processPostEvent(postEvent: PostEvent) {
        logger.info("process post {}", postEvent)
        val postOptional = messageRepository.findById(postEvent.postId ?: return@processPostEvent).blockOptional()
        if (postOptional.isPresent) {
            val post = postOptional.get()
            val user = userService.findById(post.userId).getOrNull()
            when (post.status) {
                PostStatuses.DRAFT, PostStatuses.REJECTED -> {
                    eventPublisher.publishEvent(TelegramSendMessageEvent(user?.chatId, postEvent.message))
                }

                PostStatuses.MODERATING -> {
                    eventPublisher.publishEvent(
                        TelegramSendMessageEvent(
                            user?.chatId,
                            "Пост будет добавлен после одобрения модератором: ${postService.short200(post.content)}"
                        )
                    )
                    //написать модераторам
                    val moderators = userService.listModerators()
                    moderators.forEach {
                        eventPublisher.publishEvent(
                            TelegramSendMessageEvent(
                                it.chatId,
                                post.content
                            )
                        )
                    }
                }

                PostStatuses.PUBLISHED -> {
                    eventPublisher.publishEvent(
                        TelegramSendMessageEvent(
                            user?.chatId,
                            "Пост опубликован: ${postService.short200(post.content)}"
                        )
                    )
                    eventPublisher.publishEvent(PublishDigestPostsEvent())
                }

                PostStatuses.CLOSED -> {
                    eventPublisher.publishEvent(TelegramSendMessageEvent(user?.chatId, "Пост закрыт: ${post.content}"))
                    //написать модераторам
                }

                null -> {
                    logger.info("empty status")
                }
            }
        }
    }


    companion object {
        private val logger = LoggerFactory.getLogger(PostListener::class.java)
    }

}