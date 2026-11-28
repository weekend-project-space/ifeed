package org.bitmagic.ifeed.application.search;

import org.bitmagic.ifeed.domain.repository.UserArticleStateRepository;
import org.bitmagic.ifeed.domain.service.ArticleService;
import org.bitmagic.ifeed.domain.service.UserArticleStateService;
import org.bitmagic.ifeed.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleSearchQueryServiceTest {
    @Mock private ArticleService articles;
    @Mock private SearchRetrievalService retrieval;
    @Mock private UserArticleStateRepository states;
    private ArticleSearchQueryService search;
    private final UUID feedId = UUID.randomUUID();
    private final PageRequest pageable = PageRequest.of(1, 5);

    @BeforeEach
    void setUp() {
        search = new ArticleSearchQueryService(articles, retrieval, new UserArticleStateService(states));
    }

    @Test
    void keywordSearchPreservesPersonalScopeAndNormalizesTags() {
        when(articles.searchArticles(7, "java", false, feedId, Set.of("java", "rss"), "tech", pageable))
                .thenReturn(Page.empty(pageable));
        assertThat(search.search(7, "java", "keyword", "owner", feedId.toString(), " Java, rss,Java ", "tech", pageable)
                .getContent()).isEmpty();
        verify(articles).searchArticles(7, "java", false, feedId, Set.of("java", "rss"), "tech", pageable);
        verifyNoInteractions(retrieval, states);
    }

    @Test
    void semanticSearchUsesTheSamePersonalScopeAndFilters() {
        when(retrieval.hybridSearch(8, "java", false, feedId, Set.of("java", "rss"), "tech", 1, 5))
                .thenReturn(Page.empty(pageable));
        assertThat(search.search(8, "java", "semantic", "owner", feedId.toString(), " Java, rss ", "tech", pageable)
                .getContent()).isEmpty();
        verify(retrieval).hybridSearch(8, "java", false, feedId, Set.of("java", "rss"), "tech", 1, 5);
        verifyNoInteractions(articles, states);
    }

    @Test
    void invalidScopesAreRejectedBeforeSearching() {
        assertThatThrownBy(() -> search.search(7, "java", "keyword", "other-user", null, null, null, pageable))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(articles, retrieval, states);
    }
}
