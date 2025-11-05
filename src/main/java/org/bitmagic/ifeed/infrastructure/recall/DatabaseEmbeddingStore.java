package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.application.recommendation.recall.spi.EmbeddingStore;
import org.bitmagic.ifeed.domain.model.UserEmbedding;
import org.bitmagic.ifeed.domain.repository.ArticleEmbeddingRepository;
import org.bitmagic.ifeed.domain.repository.UserEmbeddingRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 基于数据库的向量存取实现，用户向量来自 user_embeddings，物品向量来自 article_embeddings。
 */
@Component
@RequiredArgsConstructor
public class DatabaseEmbeddingStore implements EmbeddingStore {

    private final UserEmbeddingRepository userEmbeddingRepository;
    private final ArticleEmbeddingRepository articleEmbeddingRepository;

    @Override
    public Optional<float[]> getUserVector(Integer userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userEmbeddingRepository.findById(userId)
                .map(UserEmbedding::getEmbedding)
                .filter(vec -> vec != null && vec.length > 0);
    }

    @Override
    public Optional<float[]> getItemVector(Long itemId) {
        if (itemId == null) {
            return Optional.empty();
        }


        List<float[]> vectors = articleEmbeddingRepository.findAllByIds(List.of(itemId)).stream()
                .map(record -> record.embedding())
                .filter(vec -> vec != null && vec.length > 0)
                .toList();
        return vectors.isEmpty() ? Optional.empty() : Optional.of(vectors.getFirst());
    }
}
