package org.bitmagic.ifeed.domain.repository;

import org.bitmagic.ifeed.domain.entity.Article;
import org.bitmagic.ifeed.domain.entity.Feed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    List<Article> findByFeed(Feed feed);

    boolean existsByLink(String link);

    Page<Article> findByTitleContainingIgnoreCaseOrSummaryContainingIgnoreCase(String titleTerm, String summaryTerm, Pageable pageable);

    List<Article> findByIdIn(Iterable<UUID> ids);
}
