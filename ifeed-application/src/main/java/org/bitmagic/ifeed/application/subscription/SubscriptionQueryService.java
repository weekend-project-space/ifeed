package org.bitmagic.ifeed.application.subscription;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.converter.SubscriptionViewConverter;
import org.bitmagic.ifeed.api.response.SubscriptionResponse;
import org.bitmagic.ifeed.domain.model.*;
import org.bitmagic.ifeed.domain.model.value.UserSubscription;
import org.bitmagic.ifeed.domain.repository.*;
import org.bitmagic.ifeed.domain.service.SubscriptionService;
import org.bitmagic.ifeed.domain.spec.FeedSpecs;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionQueryService {
    private final SubscriptionService subscriptionService;
    private final UserReadFeedRepository userReadFeedRepository;
    private final FeedRepository feedRepository;

    public List<SubscriptionResponse> list(Integer userId) {
        if (userId == null || userId <= 0) throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        Map<String, Instant> feedReadTimes = userReadFeedRepository.findReadTimes(userId.longValue());

        List<UserSubscription> userSubscriptions = subscriptionService.getActiveSubscriptions(userId);

        Set<Integer> feedIds = userSubscriptions.stream()
                .map(UserSubscription::getSourceId)
                .collect(Collectors.toSet());

        // Fetch Feeds
        Map<Integer, Feed> id2Feed = feedIds.isEmpty() ? Map.of()
                : feedRepository.findAll(FeedSpecs.idIn(feedIds)).stream()
                        .collect(Collectors.toMap(Feed::getId, Function.identity()));

        return userSubscriptions.stream()
                .map(sub -> id2Feed.get(sub.getSourceId()))
                .filter(Objects::nonNull)
                .map(feed -> SubscriptionViewConverter.toResponse(feed, feedReadTimes, false))
                .toList();
    }

    public Page<SubscriptionResponse> list(Integer userId, Pageable pageable) {
        var values = list(userId);
        int from = (int) Math.min(pageable.getOffset(), values.size());
        int to = Math.min(from + pageable.getPageSize(), values.size());
        return new PageImpl<>(values.subList(from, to), pageable, values.size());
    }

}
