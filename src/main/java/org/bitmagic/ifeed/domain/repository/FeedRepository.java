package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.model.Feed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedRepository extends JpaRepository<Feed, Integer> {
    Optional<Feed> findByUrl(String url);

    Optional<Feed> findByUid(UUID uid);

    @Query("""
            select f from Feed f
            where lower(f.url) like lower(concat('%', :query, '%'))
               or lower(f.siteUrl) like lower(concat('%', :query, '%'))
               or lower(f.title) like lower(concat('%', :query, '%'))
            """)
    List<Feed> searchByQuery(@Param("query") String query, Pageable pageable);
}
