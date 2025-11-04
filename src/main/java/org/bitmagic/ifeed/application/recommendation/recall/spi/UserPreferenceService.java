package org.bitmagic.ifeed.application.recommendation.recall.spi;

import java.util.List;

/**
 * 用户属性偏好服务（如类目、品牌、主题等）。
 */
public interface UserPreferenceService {

    List<AttributePreference> topAttributes(Integer userId, int limit);

    record AttributePreference(String attributeKey, String attributeValue, double score) {
    }
}
