package org.bitmagic.ifeed.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.search.retrieval")
public class SearchRetrievalProperties {

    /**
     * Maximum number of BM25 candidates to fetch before fusion.
     * <p>
     * Controls the recall size from the keyword-based BM25 retriever.
     * Larger values increase recall but may impact performance.
     * </p>
     * Default: 100
     */
    private int bm25TopK = 100;

    /**
     * Maximum number of vector candidates to fetch before fusion.
     * <p>
     * Controls the recall size from the semantic vector retriever (e.g., embeddings).
     * Should typically match or be close to {@code bm25TopK} for balanced fusion.
     * </p>
     * Default: 100
     */
    private int vectorTopK = 100;

    /**
     * Maximum number of fused candidates retained before final pagination.
     * <p>
     * After combining and re-ranking BM25 + vector results, only the top N are kept.
     * This caps the final result set size before applying offset/limit.
     * </p>
     * Default: 200
     */
    private int fusionTopK = 200;

    /**
     * Weight applied to normalized BM25 score during fusion.
     * <p>
     * In the final score: {@code finalScore = bm25Weight * norm(bm25) + vectorWeight * norm(vector) + freshnessTimeWeight * freshness}
     * Must satisfy: {@code bm25Weight + vectorWeight + freshnessTimeWeight <= 1.0}
     * </p>
     * Default: 0.6
     */
    private double bm25Weight = 0.6;

    /**
     * Weight applied to normalized vector score during fusion.
     * <p>
     * See {@link #bm25Weight} for fusion formula.
     * </p>
     * Default: 0.4
     */
    private double vectorWeight = 0.4;

    /**
     * Weight applied to temporal freshness score in final ranking.
     * <p>
     * Enables time-aware ranking: newer documents get a boost.
     * Freshness score is computed using exponential decay: {@code e^(-λ × age)}.
     * The sum of all weights should not exceed 1.0.
     * </p>
     * Default: 0.3
     */
    private double freshnessTimeWeight = 0.3;

    /**
     * Decay rate (λ) for freshness score calculation.
     * <p>
     * Controls how quickly relevance decays over time.
     * Formula: {@code freshness = e^(-λ × age_in_hours)}
     * <ul>
     *   <li>Small λ (e.g., 0.01): slow decay → long-lived content stays relevant</li>
     *   <li>Large λ (e.g., 0.5): fast decay → only very recent docs get boost</li>
     * </ul>
     * </p>
     * Default: 0.01
     */
    private double freshnessLambda = 0.01;

    /**
     * Minimum similarity threshold for vector search results.
     * <p>
     * Filters out vector matches with cosine similarity (or equivalent) below this value.
     * Applied at the vector store level before fusion.
     * </p>
     * Default: 0.3
     */
    private double similarityThreshold = 0.3;
}
