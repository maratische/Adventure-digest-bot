package maratische.telegram.pvddigest.repository;

import maratische.telegram.pvddigest.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
    User findByUsername(String username);

    @Query("select u from User u where u.telegramId = ?1")
    User findByTelegramId(Long telegramId);
}
