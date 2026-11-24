package org.bitmagic.ifeed.infrastructure.recall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitmagic.ifeed.application.recommendation.recall.spi.ScoredId;
import org.bitmagic.ifeed.application.recommendation.recall.spi.SequenceStore;
import org.bitmagic.ifeed.application.recommendation.recall.spi.UserNeighborFinder;
import org.bitmagic.ifeed.domain.model.UserVectorStore;
import org.bitmagic.ifeed.domain.repository.UserVectorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于用户向量近似的邻居查找器，使用内积计算相似度并从序列存储抽取候选物品。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingUserNeighborFinder implements UserNeighborFinder {

    private final UserVectorRepository userVectorRepository;
    private final SequenceStore sequenceStore;

    @Value("${recall.u2u.neighbor-items:30}")
    private int neighborItemLimit;

    //    TODO 需要重构
    @Override
    public List<UserNeighbor> topNeighbors(Integer userId, int k) {
        if (userId == null || k <= 0) {
            return List.of();
        }

        Optional<UserVectorStore> targetOpt = userVectorRepository.findById(userId);
        if (targetOpt.isEmpty() || targetOpt.get().getEmbedding() == null) {
            return List.of();
        }

        float[] targetVector = targetOpt.get().getEmbedding();
        double targetNorm = norm(targetVector);
        if (targetNorm == 0.0d) {
            return List.of();
        }

        List<UserNeighbor> neighbors = new ArrayList<>();
        for (UserVectorStore candidate : userVectorRepository.findAll()) {
            if (candidate.getUserId().equals(userId)) {
                continue;
            }
            float[] candidateVec = candidate.getEmbedding();
            if (candidateVec == null || candidateVec.length != targetVector.length) {
                continue;
            }
            double similarity = cosine(targetVector, candidateVec, targetNorm);
            if (similarity <= 0.0d) {
                continue;
            }

            List<SequenceStore.UserInteraction> recentItems = sequenceStore.recentInteractions(candidate.getUserId(), neighborItemLimit);
            if (recentItems.isEmpty()) {
                continue;
            }

            List<ScoredId> scoredItems = recentItems.stream()
                    .map(interaction -> {
                        double weight = interaction.weight() > 0 ? interaction.weight() : 1.0;
                        Map<String, Object> meta = Map.of(
                                "timestamp", interaction.timestamp() != null ? interaction.timestamp() : Instant.EPOCH,
                                "duration", interaction.durationSeconds());
                        return new ScoredId(interaction.itemId(), similarity * weight, meta);
                    })
                    .toList();
            neighbors.add(new UserNeighbor(candidate.getUserId(), similarity, scoredItems));
        }

        neighbors.sort(Comparator.comparingDouble(UserNeighbor::similarity).reversed());
        return neighbors.size() > k ? neighbors.subList(0, k) : neighbors;
    }

    private double cosine(float[] target, float[] other, double targetNorm) {
        double dot = 0.0d;
        double otherNormSq = 0.0d;
        for (int i = 0; i < target.length; i++) {
            dot += target[i] * other[i];
            otherNormSq += other[i] * other[i];
        }
        if (otherNormSq == 0.0d) {
            return 0.0d;
        }
        return dot / (targetNorm * Math.sqrt(otherNormSq));
    }

    private double norm(float[] vector) {
        double sum = 0.0d;
        for (float v : vector) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }
}
