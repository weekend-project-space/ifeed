//package org.bitmagic.ifeed.application.recommendation;
//
//import com.rometools.utils.Strings;
//import lombok.extern.slf4j.Slf4j;
//import org.bitmagic.ifeed.application.retrieval.RetrievalContext;
//import org.bitmagic.ifeed.application.retrieval.RetrievalPipeline;
//import org.bitmagic.ifeed.application.retrieval.impl.Bm25RetrievalHandler;
//import org.bitmagic.ifeed.application.retrieval.impl.MultiChannelRetrievalPipeline;
//import org.bitmagic.ifeed.application.retrieval.impl.VectorRetrievalHandler;
//import org.bitmagic.ifeed.config.properties.RecommendationProperties;
//import org.bitmagic.ifeed.config.properties.SearchRetrievalProperties;
//import org.bitmagic.ifeed.domain.model.UserEmbedding;
//import org.bitmagic.ifeed.domain.record.ArticleSummaryView;
//import org.bitmagic.ifeed.domain.repository.UserEmbeddingRepository;
//import org.bitmagic.ifeed.domain.service.ArticleService;
//import org.bitmagic.ifeed.infrastructure.vector.VectorStoreTurbo;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.jdbc.core.JdbcTemplate;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * @author yangrd
// * @date 2025/10/28
// **/
//@Slf4j
////@Service
//public class RetrievalRecommendationService implements RecommendationService {
//
//    private final ArticleService articleService;
//    private final UserEmbeddingRepository userEmbeddingRepository;
//    private final Map<Integer, List<Long>> user2Items = new ConcurrentHashMap<>();
//    private final UserEmbedding defaultUserEmbedding;
//    private final RetrievalPipeline retrievalPipeline;
//
//    private static final String BM25_SQL = """
//             WITH query AS (
//                SELECT websearch_to_tsquery('simple', ?) AS q
//            ),
//            documents AS (
//                SELECT a.uid as id,
//                       a.pub_date,
//                       a.title,
//                       setweight(to_tsvector('simple', coalesce(a.title, '')), 'A') ||
//                       setweight(to_tsvector('simple', coalesce(a.category, '')), 'A') ||
//                       setweight(to_tsvector('simple', coalesce(a.tags, '')), 'B') ||
//                       setweight(to_tsvector('simple', coalesce(a.summary, '')), 'B') ||
//                       setweight(to_tsvector('simple', coalesce(a.author, '')), 'C') AS document
//                FROM articles a
//                WHERE (? = TRUE) OR EXISTS (
//                    SELECT 1
//                    FROM user_subscriptions us
//                    WHERE us.feed_id = a.feed_id
//                      AND us.user_id = ?
//                      AND us.is_active = TRUE
//                )
//            )
//            SELECT d.id,
//                   ts_rank_cd(d.document, query.q) AS score,
//                   d.title,
//                   d.pub_date AS pubDate
//            FROM documents d
//            CROSS JOIN query
//            WHERE query.q @@ d.document
//            ORDER BY score DESC
//            LIMIT ?
//            """;
//
//    public RetrievalRecommendationService(JdbcTemplate jdbcTemplate, VectorStoreTurbo vectorStore, RecommendationProperties recommendationProperties, SearchRetrievalProperties properties, ArticleService articleService, UserEmbeddingRepository userEmbeddingRepository) {
//        this.articleService = articleService;
//        this.userEmbeddingRepository = userEmbeddingRepository;
//        defaultUserEmbedding = new UserEmbedding(null, null, recommendationProperties.getDefaultProfile(), null);
//        this.retrievalPipeline = new MultiChannelRetrievalPipeline(properties.getFreshnessTimeWeight(), properties.getFreshnessLambda()).
//                addHandler(new Bm25RetrievalHandler(jdbcTemplate, BM25_SQL), properties.getBm25Weight())
//                .addHandler(new VectorRetrievalHandler(vectorStore, properties.getSimilarityThreshold()), properties.getVectorWeight());
////          .addHandler(new ItemCFRetrievalHandler(vectorStore, properties.getSimilarityThreshold()), properties.getVectorWeight());
//    }
//
//
//    @Override
//    public Page<ArticleSummaryView> recommend(Integer userId, int page, int size) {
//        long start = System.currentTimeMillis();
//        int safePage = Math.max(page, 0);
//        int safeSize = size <= 0 ? 10 : size;
//        return userEmbeddingRepository.findById(userId).or(() -> Optional.of(defaultUserEmbedding)).map(userEmbedding -> {
//            if (Strings.isBlank(userEmbedding.getContent())) {
//                return new PageImpl<ArticleSummaryView>(Collections.emptyList());
//            } else {
//                List<Long> cachedIds = user2Items.get(userId);
//                if (safePage == 0 || cachedIds == null) {
//                    cachedIds = retrievalPipeline.execute(RetrievalContext.builder().userId(userId).embedding(userEmbedding.getEmbedding()).query(userEmbedding.getContent()).includeGlobal(true).topK(200).build());
////                    cachedIds = searchRetrievalService.hybridSearch(userId, userEmbedding.getEmbedding(), userEmbedding.getContent(), true, -1);
//                    user2Items.put(userId, cachedIds);
//                }
//                log.info("recall:{}ms", System.currentTimeMillis() - start);
//                return articleService.findIds2Article(cachedIds, safePage, safeSize);
//            }
//
//        }).orElse(new PageImpl<>(Collections.emptyList()));
//    }
//}
