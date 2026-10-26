package org.bitmagic.ifeed.application.tracking;

import org.bitmagic.ifeed.domain.record.ArticleEmbeddingRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TopicArticleClustersTest {

    @Test
    void similarArticlesAcrossPagesShareOneClusterAndOtherArticlesRemainSeparate() {
        TopicArticleClusters clusters = new TopicArticleClusters(0.85);
        clusters.add(List.of(1L, 2L), List.of(vector(1, 1, 0), vector(2, 0, 1)));
        clusters.add(List.of(3L, 4L), List.of(vector(3, 10, 1), vector(4, -1, 0)));

        assertEquals(List.of(List.of(1L, 3L), List.of(2L), List.of(4L)), clusters.articleGroups());
    }

    @Test
    void missingInvalidAndIncompatibleVectorsRemainSingletonsWithoutDroppingArticles() {
        TopicArticleClusters clusters = new TopicArticleClusters(0.85);
        clusters.add(List.of(1L, 2L, 3L, 4L, 5L), List.of(
                vector(2, 0, 0), vector(3, Float.NaN, 1), vector(4, 1, 0), vector(5, 1, 0, 0)));

        assertEquals(List.of(List.of(1L), List.of(2L), List.of(3L), List.of(4L), List.of(5L)), clusters.articleGroups());
    }

    @Test
    void duplicateIdsDoNotAppearTwiceAndInputVectorsAreNotMutated() {
        TopicArticleClusters clusters = new TopicArticleClusters(0.85);
        float[] embedding = {3, 4};
        clusters.add(List.of(1L, 1L), List.of(new ArticleEmbeddingRecord(1L, embedding)));
        clusters.add(List.of(1L, 2L), List.of(vector(2, 6, 8)));

        assertEquals(List.of(List.of(1L, 2L)), clusters.articleGroups());
        assertEquals(3, embedding[0]);
        assertEquals(4, embedding[1]);
    }

    @Test
    void fixedRepresentativesPreventSimilarPairsFromChainingUnrelatedArticlesTogether() {
        TopicArticleClusters clusters = new TopicArticleClusters(0.8);
        clusters.add(List.of(1L, 2L, 3L), List.of(vector(1, 1, 0), vector(2, 0.866f, 0.5f), vector(3, 0.5f, 0.866f)));

        assertEquals(List.of(List.of(1L, 2L), List.of(3L)), clusters.articleGroups());
    }

    private ArticleEmbeddingRecord vector(long id, float... values) {
        return new ArticleEmbeddingRecord(id, values);
    }

    @Test
    void shuffledPagesProduceTheSameDeterministicGroups() {
        TopicArticleClusters first = new TopicArticleClusters(0.8);
        first.add(List.of(1L, 2L, 3L), List.of(vector(1, 1, 0), vector(2, .866f, .5f), vector(3, .5f, .866f)));
        TopicArticleClusters second = new TopicArticleClusters(0.8);
        second.add(List.of(3L), List.of(vector(3, .5f, .866f)));
        second.add(List.of(2L, 1L), List.of(vector(2, .866f, .5f), vector(1, 1, 0)));

        assertEquals(first.articleGroups(), second.articleGroups());
    }

    @Test
    void largeClusterIsNotLimitedByEmbeddingRequestSizeOrNeighborCandidateLimit() {
        var ids = java.util.stream.LongStream.rangeClosed(1, 600).boxed().toList();
        TopicArticleClusters clusters = new TopicArticleClusters(.85);
        for (int start = 0; start < ids.size(); start += 16) {
            List<Long> batch = ids.subList(start, Math.min(start + 16, ids.size()));
            clusters.add(batch, batch.stream().map(id -> vector(id, 1, 0)).toList());
        }

        assertEquals(List.of(ids), clusters.articleGroups());
    }
}
