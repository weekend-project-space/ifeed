package org.bitmagic.ifeed.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.domain.entity.User;
import org.bitmagic.ifeed.domain.entity.UserSession;
import org.bitmagic.ifeed.domain.repository.UserRepository;
import org.bitmagic.ifeed.domain.repository.UserSessionRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int TOKEN_BYTE_SIZE = 32;

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthToken register(String username, String rawPassword) {
        userRepository.findByUsername(username).ifPresent(user -> {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        });

        var user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build();

        user = userRepository.save(user);
        return issueToken(user);
    }

    @Transactional
    public AuthToken login(String username, String rawPassword) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return issueToken(user);
    }

    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        userSessionRepository.findByToken(token).ifPresent(userSessionRepository::delete);
    }

    public Optional<User> findUserById(UUID userId) {
        return userRepository.findById(userId);
    }

    private AuthToken issueToken(User user) {
        userSessionRepository.deleteByUser(user);

        var token = generateToken();
        var session = UserSession.builder()
                .token(token)
                .user(user)
                .build();

        userSessionRepository.save(session);
        return new AuthToken(user, token);
    }

    private String generateToken() {
        var bytes = new byte[TOKEN_BYTE_SIZE];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
