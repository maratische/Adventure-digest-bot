package maratische.telegram.pvddigest.model

import jakarta.persistence.*

@Entity
@Table(name = "posts")
class Post {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "message_id", columnDefinition = "bigint", unique = true)
    var messageId: Long? = null

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User? = null

    @Lob
    var content: String? = null

    var date: Long? = null

    var created: Long? = null
    var updated: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: PostStatuses? = null

}