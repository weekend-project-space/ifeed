package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.entity.User;
import org.bitmagic.ifeed.domain.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    Optional<UserSession> findByToken(String token);

    void deleteByUser(User user);
}
