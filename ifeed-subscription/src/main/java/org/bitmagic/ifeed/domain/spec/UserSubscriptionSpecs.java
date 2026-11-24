package org.bitmagic.ifeed.domain.spec;

import org.bitmagic.ifeed.domain.model.SourceType;
import org.bitmagic.ifeed.domain.model.value.UserSubscription;
import org.bitmagic.ifeed.infrastructure.spec.Spec;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.Objects;

/**
 * @author yangrd
 * @date 2025/11/8
 **/
public interface UserSubscriptionSpecs {

    static Specification<UserSubscription> userAndSourceTypeAndSourceIdsActive(Integer userId, SourceType sourceType,
            Collection<Integer> sourceIds) {
        return Spec.<UserSubscription>on()
                .when(Objects.nonNull(userId), b -> b.join("user").eq("id", userId))
                .eq("sourceType", sourceType)
                .when(Objects.nonNull(sourceIds) && !sourceIds.isEmpty(), b -> b.in("sourceId", sourceIds))
                .isTrue("active").build();
    }
}
