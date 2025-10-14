package org.bitmagic.ifeed.domain.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_behavior")
public class UserBehaviorDocument {

    @Id
    private String id;

    @Builder.Default
    private List<ArticleRef> collections = new ArrayList<>();

    @Builder.Default
    private List<ArticleRef> readHistory = new ArrayList<>();

    @Builder.Default
    private List<Interaction> interactionHistory = new ArrayList<>();

    @Builder.Default
    private List<FeedRef> readFeedHistory = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedRef {
        private String feedId;
        private Instant timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleRef {
        private String articleId;
        private Instant timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Interaction {
        private String articleId;
        private String actionType;
        private Instant timestamp;
    }
}
