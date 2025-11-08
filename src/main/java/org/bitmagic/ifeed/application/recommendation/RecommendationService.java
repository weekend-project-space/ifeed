package org.bitmagic.ifeed.application.recommendation;

import org.springframework.data.domain.Page;

/**
 * @author yangrd
 * @date 2025/10/28
 **/
public interface RecommendationService {

    Page<RecResponse> recommend(RecRequest request, int page, int size);
}
