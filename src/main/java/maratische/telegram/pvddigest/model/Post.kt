package maratische.telegram.pvddigest.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table


@Table(name = "posts")
class Post {
    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(value = "message_id")
//    @Column(name = "message_id", columnDefinition = "bigint", unique = true)
    var messageId: Long? = null

    @Column(value = "user_id")
//    @Column(name = "user_id", columnDefinition = "bigint")
    var userId: Long? = null

    //    @Lob
    var content: String? = null

    var date: Long? = null

    var created: Long? = null
    var updated: Long? = null

    //    @Enumerated(EnumType.STRING)
//@Column(name = "status")
    @Column(value = "status")
    var status: PostStatuses? = null

    constructor()

    constructor(messageId: Long, userId: Long?) {
        this.messageId = messageId
        this.userId = userId
        this.created = System.currentTimeMillis()
    }
}