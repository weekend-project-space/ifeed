package org.bitmagic.ifeed.application.recommendation;

import org.bitmagic.ifeed.domain.record.ArticleSummaryView;
import org.springframework.data.domain.Page;

/**
 * @author yangrd
 * @date 2025/10/28
 **/
public interface RecommendationService {

    Page<ArticleSummaryView> recommend(Integer userId, int page, int size);
}
