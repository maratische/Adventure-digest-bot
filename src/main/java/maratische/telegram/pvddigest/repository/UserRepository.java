package maratische.telegram.pvddigest.repository;

import maratische.telegram.pvddigest.model.User;
import maratische.telegram.pvddigest.model.UserRoles;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
    User findByUsername(String username);

    @Query("select u from User u where u.telegramId = ?1")
    User findByTelegramId(Long telegramId);

    @Query("select u from User u where u.role = ?1")
    List<User> findByRole(UserRoles role);

}
