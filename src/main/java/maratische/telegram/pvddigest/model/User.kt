package maratische.telegram.pvddigest.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table


//@Entity
@Table(name = "users")
class User {
    @Id
    @Column(value = "id")
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    //    @Column(name = "username", columnDefinition = "varchar(255)")
    @Column(value = "username")
    var username: String? = null

    //    @Column(name = "firstname", columnDefinition = "varchar(255)")
    @Column(value = "firstname")
    var firstname: String? = null

    //    @Column(name = "telegram_id", columnDefinition = "bigint", unique = true)
    @Column(value = "telegram_id")
    var telegramId: Long? = null

    //    @Enumerated(EnumType.STRING)
    @Column(value = "role")
    var role: UserRoles? = null

    /**
     * сообщения в личку
     */
//    @Column(name = "chat_id", columnDefinition = "bigint", unique = true)
    @Column(value = "chat_id")
    var chatId: Long? = null

    override fun toString(): String = "User($id, $username, $firstname, $telegramId, $role, $chatId)"
}
