package myavocat.legit.security;

import myavocat.legit.model.User;
import myavocat.legit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("userSecurity")
public class UserSecurity {

    @Autowired
    private UserRepository userRepository;

    public boolean isCurrentUser(UUID userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        String currentUsername = authentication.getName();
        User user = userRepository.findById(userId).orElse(null);

        return user != null && user.getEmail().equals(currentUsername);
    }
}