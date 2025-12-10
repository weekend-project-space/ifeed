package org.bitmagic.ifeed.application.recommendation;

import java.util.Map;

/**
 * @author yangrd
 * @date 2025/11/8
 **/
public record RecRequest(Integer userId, String scene,Map<String, Object> context,Map<String, Object> filters) {
}
