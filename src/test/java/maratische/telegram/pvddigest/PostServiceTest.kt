package maratische.telegram.pvddigest

import maratische.telegram.pvddigest.model.PostStatuses
import maratische.telegram.pvddigest.model.User
import maratische.telegram.pvddigest.model.UserRoles
import maratische.telegram.pvddigest.repository.PostRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime
import java.time.ZoneId

class PostServiceTest {

    @Mock
    private lateinit var messageRepository: PostRepository

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Mock
    private lateinit var userService: UserService

    @InjectMocks
    private lateinit var messageService: PostService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun parseDate() {
        assertEquals(
            LocalDateTime.of(2024, 11, 1, 0, 0).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
            messageService.parseDate("Привет! #pvd едем в #2024-11-01 и все!")
        )
        assertEquals(
            LocalDateTime.of(2024, 11, 1, 0, 0).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
            messageService.parseDate("Привет! #pvd едем в #2024-11-01")
        )
        assertEquals(
            LocalDateTime.of(2024, 11, 1, 0, 0).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
            messageService.parseDate("#2024-11-01 и все!")
        )
        assertEquals(
            LocalDateTime.of(2024, 11, 1, 0, 0).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
            messageService.parseDate("#2024-11-1 и все!")
        )
        assertEquals(
            0,
            messageService.parseDate("#2024-11 и все!")
        )
        assertEquals(
            LocalDateTime.of(2024, 11, 1, 12, 0).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
            messageService.parseDate("Привет! #pvd едем в #2024-11-01T12:00 и все!")
        )
        assertEquals(
            LocalDateTime.of(2024, 11, 1, 12, 0).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
            messageService.parseDate("Привет! #pvd едем в #2024-11-01Т12:00 и все!")
        )
    }

    /**
     * пост от новичка содержит пвд и дату - отправляется на модерацию
     */
    @Test
    fun processMessage1() {
        var user = User()
        user.id = 1
        user.username = "test"
        user.role = UserRoles.BEGINNER

        var message = Message(1L, null, null, System.currentTimeMillis(), "text #pvd #2024-11-29 test", null);
        `when`(messageRepository.save(any(maratische.telegram.pvddigest.model.Post::class.java)))
            .thenAnswer { t -> t.arguments[0] }

        var messageDB = messageService.processMessage(message, user) ?: throw RuntimeException("Null")
        assertEquals(messageDB.status, PostStatuses.MODERATING)

    }

    /**
     * пост от новичка содержит пвд и нет даты - отправляется в черновик
     */
    @Test
    fun processMessage2() {
        var user = User()
        user.id = 1
        user.username = "test"
        user.role = UserRoles.BEGINNER

        var message = Message(1L, null, null, System.currentTimeMillis(), "text #pvd 2024-11-29 test", null);
        `when`(messageRepository.save(any(maratische.telegram.pvddigest.model.Post::class.java)))
            .thenAnswer { t -> t.arguments[0] }

        var messageDB = messageService.processMessage(message, user) ?: throw RuntimeException("Null")
        assertEquals(messageDB.status, PostStatuses.DRAFT)

    }

    /**
     * пост от бывалого содержит пвд и  дату - отправляется в публикацию
     */
    @Test
    fun processMessage3() {
        var user = User()
        user.id = 1
        user.username = "test"
        user.role = UserRoles.TRAVELER

        var message = Message(1L, null, null, System.currentTimeMillis(), "text #pvd #2024-11-29 test", null);
        `when`(messageRepository.save(any(maratische.telegram.pvddigest.model.Post::class.java)))
            .thenAnswer { t -> t.arguments[0] }

        var messageDB = messageService.processMessage(message, user) ?: throw RuntimeException("Null")
        assertEquals(messageDB.status, PostStatuses.PUBLISHED)

    }

}