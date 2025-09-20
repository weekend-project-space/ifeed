package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.entity.Feed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FeedRepository extends JpaRepository<Feed, UUID> {
    Optional<Feed> findByUrl(String url);
}
