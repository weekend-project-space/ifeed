package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.model.UserSession;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    @Cacheable(cacheNames = "USER-SESSIONS", key = "#p0", unless = "#result == null")
    Optional<UserSession> findByToken(String token);

    @CacheEvict(cacheNames = "USER-SESSIONS", key = "#p0")
    void deleteByToken(String token);

    Optional<UserSession> findByUser(User user);
}
