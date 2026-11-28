package org.bitmagic.ifeed.api.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.UserResponse;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.application.user.UserPlanApplicationService;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserPlanApplicationService plans;

    //    src="https://api.dicebear.com/7.x/avataaars/svg?seed=13437977913"
    @GetMapping("/user")
    public ResponseEntity<UserResponse> currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return ResponseEntity.ok(new UserResponse(principal.getId().toString(), principal.getUsername(), "https://api.dicebear.com/7.x/pixel-art/svg?seed=" + principal.getUsername(), principal.getCurrentPlan().name()));
    }

    @PostMapping("/plan")
    @PreAuthorize("hasRole('ADMIN')")
    public void updateUserPlan(@AuthenticationPrincipal UserPrincipal principal, @RequestBody UserReq req) {
        plans.updatePlan(req.getUsername(), req.getPlan());
    }

    @Data
    static class UserReq {
        private String username;
        private User.Plan plan;
    }
}
