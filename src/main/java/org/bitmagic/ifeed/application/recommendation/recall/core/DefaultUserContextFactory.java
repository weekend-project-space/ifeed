package org.bitmagic.ifeed.application.recommendation.recall.core;

import org.bitmagic.ifeed.application.recommendation.recall.model.RecallRequest;
import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;
import org.bitmagic.ifeed.application.recommendation.recall.spi.SequenceStore;

import java.util.List;

/**
 * 基础用户上下文构造器，仅使用请求中的字段，可按需扩展画像查询。
 */
public class DefaultUserContextFactory implements UserContextFactory {

    private final SequenceStore sequenceStore;

    public DefaultUserContextFactory(SequenceStore sequenceStore) {
        this.sequenceStore = sequenceStore;
    }

    @Override
    public UserContext create(RecallRequest request) {
        Integer userId = request.userId();
        List<SequenceStore.UserInteraction> interactions = sequenceStore != null
                ? sequenceStore.recentInteractions(userId, 150)
                : List.of();
        return new UserContext(userId, request.scene(), interactions, request.filters(), request.requestTime());
    }
}
