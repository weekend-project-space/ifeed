package org.bitmagic.ifeed.application.recommendation;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * @author yangrd
 * @date 2025/10/28
 **/
public interface RecommendationService {

    Page<RecResponse> recommend(RecRequest request, int page, int size);

    List<RecResponse> recommend(RecRequest request, int topK);
}
