package maratische.telegram.pvddigest;

import jakarta.annotation.PostConstruct;
import maratische.telegram.pvddigest.model.User;
import maratische.telegram.pvddigest.model.UserRoles;
import maratische.telegram.pvddigest.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Value("${pvddigest.admin}")
    private String admin;

    @PostConstruct
    public void init() {
        if (!StringUtils.isEmpty(admin)) {
            var user = userRepository.findByUsername(admin);
            if (user != null && user.getRole() != UserRoles.ADMIN) {
                user.setRole(UserRoles.ADMIN);
                userRepository.save(user);
            }
        }
    }

    public User getOtCreateUser(MessageUser messageUser) {
        if (messageUser != null) {
            var user = userRepository.findByTelegramId(messageUser.getId());
            if (user == null) {
                user = new User();
            }
            if (!StringUtils.equals(user.getUsername(), messageUser.getUsername())) {
                user.setTelegramId(messageUser.getId());
                user.setUsername(messageUser.getUsername());
                user.setRole(UserRoles.BEGINNER);
                return save(user);
            }
            return user;
        }
        return null;
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}
