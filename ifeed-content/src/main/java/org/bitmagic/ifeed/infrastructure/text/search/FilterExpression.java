package org.bitmagic.ifeed.infrastructure.text.search;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource; /**
 * 过滤表达式接口
 */
public interface FilterExpression {
    String toFilterString();
    void addParameters(MapSqlParameterSource params);
}
