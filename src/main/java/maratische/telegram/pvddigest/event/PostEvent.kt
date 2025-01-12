package maratische.telegram.pvddigest.event

/**
 * изменние статуса поста
 */
class PostEvent(
    var postId: Long?,
    var message: String? = null,
) {
    override fun toString(): String {
        return "PostEvent($postId, $message)"
    }
}