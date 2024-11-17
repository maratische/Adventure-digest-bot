package maratische.telegram.pvddigest.event

/**
 * изменние статуса поста
 */
class PostEvent(
    val postId: Long?,
    val message: String? = null,
) {
}