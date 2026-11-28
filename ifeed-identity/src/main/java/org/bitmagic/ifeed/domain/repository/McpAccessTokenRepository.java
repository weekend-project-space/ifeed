package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.model.McpAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface McpAccessTokenRepository extends JpaRepository<McpAccessToken, UUID> {
    List<McpAccessToken> findAllByUser_IdOrderByCreatedAtDesc(Integer userId);
    Optional<McpAccessToken> findByTokenIdAndUser_IdAndExpiresAtAfter(UUID tokenId, Integer userId, Instant now);
    void deleteByExpiresAtBefore(Instant instant);
}
