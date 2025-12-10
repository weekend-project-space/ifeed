package org.bitmagic.ifeed.application.recommendation.recall.core;

import org.bitmagic.ifeed.application.recommendation.recall.model.RecallRequest;
import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;

/**
 * 用户上下文工厂，用于在召回前准备完整的上下文数据。
 */
public interface UserContextFactory {

    UserContext create(RecallRequest request);
}
