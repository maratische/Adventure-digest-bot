package maratische.telegram.pvddigest.model

import jakarta.persistence.*

@Entity
@Table(name = "users")
class User {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "username", columnDefinition = "varchar(255)")
    var username: String? = null

    @Column(name = "telegram_id", columnDefinition = "bigint", unique = true)
    var telegramId: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    var role: UserRoles? = null

    /**
     * сообщения в личку
     */
    @Column(name = "chat_id", columnDefinition = "bigint", unique = true)
    var chatId: Long? = null
}
