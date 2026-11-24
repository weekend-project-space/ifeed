package org.bitmagic.ifeed.domain.spec;

import org.apache.logging.log4j.util.Strings;
import org.bitmagic.ifeed.domain.model.Feed;
import org.bitmagic.ifeed.infrastructure.spec.Spec;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

/**
 * @author yangrd
 * @date 2025/11/8
 **/
public interface FeedSpecs {

    static Specification<Feed> urlIn(Collection<String> urls) {
        return Spec.<Feed>on().in("url", urls).build();
    }

    static Specification<Feed> idIn(Collection<Integer> ids) {
        return Spec.<Feed>on().in("id", ids).build();
    }

    static Specification<Feed> specLastUpdatedNotNull(String category) {
        return Spec.<Feed>on().notNull("lastUpdated").when(Strings.isNotBlank(category),b -> b.eq("category", category)).build();
    }
}
