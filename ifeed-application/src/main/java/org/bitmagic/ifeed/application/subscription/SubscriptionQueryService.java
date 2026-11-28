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
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionQueryService {
    private final SubscriptionService subscriptionService;
    private final UserReadFeedRepository userReadFeedRepository;
    private final FeedRepository feedRepository;
    private final MixFeedRepository mixFeedRepository;

    public List<SubscriptionResponse> list(Integer userId) {
        if (userId == null || userId <= 0) throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        Map<String, Instant> feedReadTimes = userReadFeedRepository.findReadTimes(userId.longValue());

        List<UserSubscription> userSubscriptions = subscriptionService.getActiveSubscriptions(userId);

        // Separate IDs by type
        Set<Integer> feedIds = new HashSet<>();
        Set<Integer> mixFeedIds = new HashSet<>();

        for (UserSubscription sub : userSubscriptions) {
            if (sub.getSourceType() == SourceType.FEED) {
                feedIds.add(sub.getSourceId());
            } else if (sub.getSourceType() == SourceType.MIX_FEED) {
                mixFeedIds.add(sub.getSourceId());
            }
        }

        // Fetch Feeds
        Map<Integer, Feed> id2Feed = feedIds.isEmpty() ? Map.of()
                : feedRepository.findAll(FeedSpecs.idIn(feedIds)).stream()
                        .collect(Collectors.toMap(Feed::getId, Function.identity()));

        // Fetch MixFeeds
        Map<Integer, MixFeed> id2MixFeed = mixFeedIds.isEmpty() ? Map.of()
                : mixFeedRepository.findAllById(mixFeedIds).stream()
                        .collect(Collectors.toMap(MixFeed::getId, Function.identity()));

        // Map to response
        List<SubscriptionResponse> responses = userSubscriptions.stream()
                .flatMap(sub -> {
                    if (sub.getSourceType() == SourceType.FEED) {
                        Feed feed = id2Feed.get(sub.getSourceId());
                        if (feed != null) {
                            return Stream.of(SubscriptionViewConverter.toResponse(feed, feedReadTimes, false));
                        }
                    } else if (sub.getSourceType() == SourceType.MIX_FEED) {
                        MixFeed mixFeed = id2MixFeed.get(sub.getSourceId());
                        if (mixFeed != null) {
                            return Stream.of(SubscriptionViewConverter.toResponse(mixFeed, feedReadTimes, false));
                        }
                    }
                    return Stream.empty();
                })
                .toList();

        return responses;
    }

    public Page<SubscriptionResponse> list(Integer userId, Pageable pageable) {
        var values = list(userId);
        int from = (int) Math.min(pageable.getOffset(), values.size());
        int to = Math.min(from + pageable.getPageSize(), values.size());
        return new PageImpl<>(values.subList(from, to), pageable, values.size());
    }

}
