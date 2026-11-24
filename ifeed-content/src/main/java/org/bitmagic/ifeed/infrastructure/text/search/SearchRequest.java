package org.bitmagic.ifeed.infrastructure.text.search;

/**
 * 搜索请求
 */
public record SearchRequest(
        String query,
        int topK,
        double similarityThreshold,
        FilterExpression filterExpression
) {
    public SearchRequest {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or blank");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("TopK must be positive");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String query;
        private int topK = 10;
        private double similarityThreshold = 0.0;
        private FilterExpression filterExpression;

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder topK(int topK) {
            this.topK = topK;
            return this;
        }

        public Builder similarityThreshold(double threshold) {
            this.similarityThreshold = threshold;
            return this;
        }

        public Builder filterExpression(FilterExpression filterExpression) {
            this.filterExpression = filterExpression;
            return this;
        }

        public SearchRequest build() {
            return new SearchRequest(query, topK, similarityThreshold, filterExpression);
        }
    }
}
