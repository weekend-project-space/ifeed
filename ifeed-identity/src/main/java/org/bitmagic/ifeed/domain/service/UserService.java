package org.bitmagic.ifeed.domain.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.repository.UserRepository;
import org.bitmagic.ifeed.domain.repository.UserSessionRepository;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author yangrd
 * @date 2025/12/16
 **/
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final CacheManager cacheManager;

    @Cacheable(cacheNames = "USERS", key = "#p0", unless = "#result == null")
    public Optional<User> findUserById(Integer userId) {
        return userRepository.findById(userId);
    }

    @CacheEvict(cacheNames = "USERS", key = "#p0.id")
    public void updateUser(User user) {
//      移除token
        userSessionRepository.findByUser(user).ifPresent(userSession -> {
            var cache = cacheManager.getCache("USER-SESSIONS");
            if (cache != null) {
                cache.evict(userSession.getToken());
            }
        });
        userRepository.save(user);
    }
}
