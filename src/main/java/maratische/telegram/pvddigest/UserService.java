package maratische.telegram.pvddigest;

import maratische.telegram.pvddigest.model.User;
import maratische.telegram.pvddigest.model.UserRoles;
import maratische.telegram.pvddigest.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private UserRepository userRepository;

    @Value("${pvddigest.admin}")
    private String admin;

    public Mono<User> processAdmin() {
        if (!StringUtils.isEmpty(admin)) {
            return userRepository.findByUsername(admin).filter(
                    user -> user.getRole() != UserRoles.ADMIN
            ).flatMap(
                    user -> {
                        user.setRole(UserRoles.ADMIN);
                        return userRepository.save(user);
                    }
            );
        }
        return Mono.empty();
    }

    //    @Transactional
    public Mono<User> getOtCreateUser(MessageUser messageUser) {
        if (messageUser == null) {
            return Mono.empty();
        }
        return userRepository.findByTelegramId(messageUser.getId()).switchIfEmpty(Mono.just(new User()))
                .flatMap(
                        user -> {
                            if (!StringUtils.equals(user.getUsername(), messageUser.getUsername())
                                    || !StringUtils.equals(user.getFirstname(), messageUser.getFirst_name())) {
                                user.setTelegramId(messageUser.getId());
                                user.setUsername(messageUser.getUsername());
                                user.setFirstname(messageUser.getFirst_name());
                                user.setRole(UserRoles.BEGINNER);
                                return save(user);
                            }
                            return Mono.just(user);
                        }
                );
    }

    public List<User> listModerators() {
        ArrayList<User> users = new ArrayList<>();
        users.addAll(userRepository.findByRole(UserRoles.MODERATOR).toStream().toList());
        users.addAll(userRepository.findByRole(UserRoles.ADMIN).toStream().toList());
        return users;
    }

    public Flux<User> list() {
//        ArrayList<User> users = new ArrayList<>();
        return userRepository.findAll();
    }

    public Optional<User> findById(Long userId) {
        return Optional.ofNullable(userRepository.findById(userId).block());
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).block();
    }

    public User findByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId).block();
    }

    public Mono<User> save(User user) {
        return userRepository.save(user);
    }
}
