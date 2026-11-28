package org.bitmagic.ifeed.domain.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.repository.UserRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.infrastructure.spec.Spec;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthToken register(String username, String rawPassword) {
        userRepository.findByUsername(username).ifPresent(user -> {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        });

        var user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .currentPlan(User.Plan.FREE)
                .currentPlanCreatedAt(Instant.now())
                .role(User.UserRole.USER)
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
    public AuthToken authLogin(String id, String username) {
        var user = userRepository.findOne(Spec.<User>on().eq("linuxDoUserId", id).build())
                .orElseGet(() -> {
                    String finalUsername = username;

                    // 如果用户名冲突，使用 username_linuxdo{id} 格式
                    if (userRepository.findByUsername(username).isPresent()) {
                        finalUsername = username + "_linux_do" + id;
                    }

                    User u = User.builder()
                            .linuxDoUserId(id)
                            .username(finalUsername)
                            .passwordHash(finalUsername)
                            .currentPlan(User.Plan.FREE)
                            .currentPlanCreatedAt(Instant.now())
                            .build();

                    return userRepository.save(u);
                });

        return issueToken(user);
    }

    @Transactional
    public void logout(String token) {
        // Access JWTs expire naturally; the client discards its local copy on logout.
    }

    private AuthToken issueToken(User user) {
        var token = jwtTokenService.issueWeb(user);
        return new AuthToken(user, token);
    }
}
