package org.bitmagic.ifeed.application.subscription;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.request.OpmlImportConfirmRequest;
import org.bitmagic.ifeed.api.request.SubscriptionRequest;
import org.bitmagic.ifeed.api.response.OpmlImportConfirmResponse;
import org.bitmagic.ifeed.api.response.OpmlPreviewResponse;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.service.OpmlImportService;
import org.bitmagic.ifeed.domain.service.SubscriptionService;
import org.bitmagic.ifeed.domain.service.UserService;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
@RequiredArgsConstructor
public class SubscriptionApplicationService {
    private final SubscriptionService subscriptionService;
    private final UserService userService;
    private final OpmlImportService opmlImportService;

    public void subscribe(Integer userId, SubscriptionRequest request) {
        subscriptionService.subscribe(resolveUser(userId), request);
    }

    public void unsubscribe(Integer userId, String sourceId) {
        subscriptionService.unsubscribe(resolveUser(userId), sourceId);
    }

    public OpmlPreviewResponse preview(Integer userId, MultipartFile file) {
        return opmlImportService.generatePreview(resolveUser(userId), file);
    }

    public OpmlImportConfirmResponse confirm(Integer userId, OpmlImportConfirmRequest request) {
        return opmlImportService.confirm(resolveUser(userId), request);
    }

    private User resolveUser(Integer userId) {
        if (userId == null || userId <= 0) throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        return userService.findUserById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

}
