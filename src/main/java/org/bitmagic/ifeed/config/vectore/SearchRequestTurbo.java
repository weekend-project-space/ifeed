package org.bitmagic.ifeed.config.vectore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.lang.Nullable;

/**
 * @author yangrd
 * @date 2025/10/22
 **/
@Builder
@AllArgsConstructor
public class SearchRequestTurbo {

    private float[] embedding;
    private int topK = 4;
    private double similarityThreshold = 0.0;
    @Nullable
    private Filter.Expression filterExpression;

    public SearchRequestTurbo() {
    }

    public int getTopK() {
        return this.topK;
    }

    public double getSimilarityThreshold() {
        return this.similarityThreshold;
    }

    @Nullable
    public Filter.Expression getFilterExpression() {
        return this.filterExpression;
    }

    public boolean hasFilterExpression() {
        return this.filterExpression != null;
    }

    public float[] getEmbedding() {
        return embedding;
    }
}
