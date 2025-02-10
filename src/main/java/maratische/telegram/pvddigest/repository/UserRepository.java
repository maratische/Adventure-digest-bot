package maratische.telegram.pvddigest.repository;

import maratische.telegram.pvddigest.model.User;
import maratische.telegram.pvddigest.model.UserRoles;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByUsername(String username);

    @Query("select * from users u where u.telegram_id = :telegramId")
    Mono<User> findByTelegramId(Long telegramId);

    @Query("select * from users u where u.role = :role")
    Flux<User> findByRole(UserRoles role);

}
