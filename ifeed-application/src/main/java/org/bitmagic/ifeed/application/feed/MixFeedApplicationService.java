package org.bitmagic.ifeed.application.feed;

import org.bitmagic.ifeed.api.request.MixFeedRequest;
import org.bitmagic.ifeed.api.request.SubscriptionRequest;
import org.bitmagic.ifeed.api.response.MixFeedDetailResponse;
import org.bitmagic.ifeed.api.response.MixFeedListResponse;
import org.bitmagic.ifeed.domain.model.MixFeed;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.service.MixFeedService;
import org.bitmagic.ifeed.domain.service.SubscriptionService;
import org.bitmagic.ifeed.domain.service.UserService;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MixFeedApplicationService {
    private final MixFeedService mixFeedService;
    private final UserService userService;
    private final SubscriptionService subscriptionService;


    public void create(Integer userId, MixFeedRequest request) {
        User user = resolveUser(userId);
        MixFeed mixFeed = mixFeedService.create(user, request.name(), request.description(), request.icon(),
                request.isPublic() != null && request.isPublic(), request.filterConfig());
        subscriptionService.subscribe(user, SubscriptionRequest.of(mixFeed.getUid().toString()));
    }

    public List<MixFeedListResponse> listMyMixFeeds(Integer userId) {
        ensureAuthenticated(userId);
        List<MixFeed> mixFeeds = mixFeedService.getMyMixFeeds(userId);
        List<MixFeedListResponse> responses = mixFeeds.stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());
        return responses;
        }

    public MixFeedDetailResponse getDetail(Integer userId, UUID id) {
        ensureAuthenticated(userId);
        var detail = mixFeedService.getDetail(id, userId);
        var subscribed = mixFeedService.isSubscribed(userId, detail.mixFeed());
        return toDetailResponse(detail, subscribed);
        }

    public void update(Integer userId, UUID id, MixFeedRequest request) {
        ensureAuthenticated(userId);
        mixFeedService.update(id, userId, request.name(), request.description(), request.icon(), request.isPublic(), request.filterConfig());
    }

    public void delete(Integer userId, UUID id) {
        mixFeedService.delete(id, resolveUser(userId).getId());
    }

    public Page<MixFeedListResponse> listPublicMixFeeds(Pageable pageable) {
        Page<MixFeed> mixFeeds = mixFeedService.getPublicMixFeeds(pageable);
        return mixFeeds.map(this::toListResponse);
        }

    private void ensureAuthenticated(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private User resolveUser(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userService.findUserById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private MixFeedListResponse toListResponse(MixFeed mixFeed) {
        return new MixFeedListResponse(
                mixFeed.getUid().toString(),
                mixFeed.getName(),
                mixFeed.getDescription(),
                mixFeed.getIcon(),
                mixFeed.getSubscriberCount(),
                mixFeed.getIsPublic(),
                mixFeed.getCreatedAt());
    }

    private MixFeedDetailResponse toDetailResponse(MixFeedService.MixFeedDetail detail, boolean subscribed) {
        MixFeed mixFeed = detail.mixFeed();
        return new MixFeedDetailResponse(
                mixFeed.getUid().toString(),
                mixFeed.getName(),
                mixFeed.getDescription(),
                mixFeed.getIcon(),
                detail.subscriberCount(),
                detail.articleCount(),
                mixFeed.config(),
                subscribed,
                mixFeed.getIsPublic(),
                mixFeed.getCreatedAt(),
                mixFeed.getUpdatedAt());
    }

}
