package maratische.telegram.pvddigest.event

/**
 * изменние статуса поста
 */
class PostEvent(
    val postId: Long?,
    val message: String? = null,
) {
    override fun toString(): String {
        return "PostEvent($postId, $message)"
    }
}