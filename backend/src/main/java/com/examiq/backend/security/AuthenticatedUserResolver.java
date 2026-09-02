package com.examiq.backend.security;

import com.examiq.backend.entity.User;
import com.examiq.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserResolver {

    private final UserRepository userRepository;

    public AuthenticatedUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        return getCurrentUser(SecurityContextHolder.getContext().getAuthentication());
    }

    public User getCurrentUser(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("User not authenticated");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }
}
