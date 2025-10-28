package org.bitmagic.ifeed.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "search.retrieval")
public class SearchRetrievalProperties {

    /**
     * Maximum number of BM25 candidates to fetch before fusion.
     */
    private int bm25TopK = 100;

    /**
     * Maximum number of vector candidates to fetch before fusion.
     */
    private int vectorTopK = 100;

    /**
     * Maximum number of fused candidates retained before paging.
     */
    private int fusionTopK = 200;

    /**
     * Weight applied to normalized BM25 score during fusion.
     */
    private double bm25Weight = 0.6;

    /**
     * Weight applied to normalized vector score during fusion.
     */
    private double vectorWeight = 0.4;

    /**
     * Similarity threshold passed to the vector store.
     */
    private double similarityThreshold = 0.3;
}
