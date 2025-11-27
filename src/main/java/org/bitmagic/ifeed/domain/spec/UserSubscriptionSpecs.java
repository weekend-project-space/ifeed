package org.bitmagic.ifeed.domain.spec;

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

    static Specification<UserSubscription> userAndFeedIdsActive(Integer userId, Collection<Integer> feedIds) {
        return Spec.<UserSubscription>on()
                .when(Objects.nonNull(userId), b -> b.join("user").eq("id", userId))
                .when(Objects.nonNull(feedIds) && !feedIds.isEmpty(), b -> b.join("feed").in("id", feedIds))
                .isTrue("active").build(); // 永远加 active
    }
}
