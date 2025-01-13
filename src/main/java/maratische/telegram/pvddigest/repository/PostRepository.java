package maratische.telegram.pvddigest.repository;

import maratische.telegram.pvddigest.model.Post;
import maratische.telegram.pvddigest.model.PostStatuses;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends CrudRepository<Post, Long> {

    @Query("select u from Post u where u.messageId = ?1")
    Post findByMessageId(Long messageId);

    @Query("select u from Post u where u.status = ?1")
    List<Post> findByStatus(PostStatuses status);

    @Query("select u from Post u where u.userId = ?1 and u.status = ?2")
    List<Post> findByUserAndStatus(Long userId, PostStatuses status);

}
