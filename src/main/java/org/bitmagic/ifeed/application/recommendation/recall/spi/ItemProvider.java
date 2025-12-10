package org.bitmagic.ifeed.application.recommendation.recall.spi;

import org.bitmagic.ifeed.application.recommendation.recall.model.UserContext;

import java.util.List;

/**
 * @author yangrd
 * @date 2025/11/10
 **/
public interface ItemProvider {

    List<ScoredId> ls(UserContext userContext, ScoredLsType type, Integer k);

    enum ScoredLsType {
        LATEST,
        RANDOM,
        HOT,
    }
}
