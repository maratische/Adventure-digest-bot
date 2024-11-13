package maratische.telegram.pvddigest.repository;

import maratische.telegram.pvddigest.model.Post;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends CrudRepository<Post, Long> {

    @Query("select u from Post u where u.messageId = ?1")
    Post findByMessageId(Long messageId);
}
