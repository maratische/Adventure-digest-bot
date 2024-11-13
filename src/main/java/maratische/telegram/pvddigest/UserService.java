package maratische.telegram.pvddigest;

import maratische.telegram.pvddigest.model.User;
import maratische.telegram.pvddigest.model.UserRoles;
import maratische.telegram.pvddigest.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

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
