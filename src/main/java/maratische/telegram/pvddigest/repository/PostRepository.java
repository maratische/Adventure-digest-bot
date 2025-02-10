package maratische.telegram.pvddigest.repository;

import maratische.telegram.pvddigest.model.Post;
import maratische.telegram.pvddigest.model.PostStatuses;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PostRepository extends ReactiveCrudRepository<Post, Long> {

    @Query("select * from posts u where u.message_Id = ?1")
    Mono<Post> findByMessageId(Long messageId);

    @Query("select * from posts u where u.status = ?1")
    Flux<Post> findByStatus(PostStatuses status);

    @Query("select * from posts u where u.user_Id = ?1 and u.status = ?2")
    Flux<Post> findByUserAndStatus(Long userId, PostStatuses status);

}
