package org.bitmagic.ifeed.service.recommendation;

import org.bitmagic.ifeed.domain.projection.ArticleSummaryView;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * @author yangrd
 * @date 2025/10/28
 **/
public interface RecommendationService {

    Page<ArticleSummaryView> recommend(UUID userId, int page, int size);
}
