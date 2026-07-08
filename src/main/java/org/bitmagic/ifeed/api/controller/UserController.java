package org.bitmagic.ifeed.api.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.response.UserResponse;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.repository.UserRepository;
import org.bitmagic.ifeed.domain.service.UserService;
import org.bitmagic.ifeed.exception.ApiException;
import org.bitmagic.ifeed.config.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    private final UserService userService;

    //    src="https://api.dicebear.com/7.x/avataaars/svg?seed=13437977913"
    @GetMapping("/user")
    public ResponseEntity<UserResponse> currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return ResponseEntity.ok(new UserResponse(principal.getId().toString(), principal.getUsername(), "https://api.dicebear.com/7.x/pixel-art/svg?seed=" + principal.getUsername(), principal.getCurrentPlan().name()));
    }

    @PostMapping("/plan")
    public void updateUserPlan(@AuthenticationPrincipal UserPrincipal principal, @RequestBody UserReq req) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (!principal.getUsername().equals("yangrd")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
        userRepository.findByUsername(req.getUsername()).ifPresent(user -> {
            user.changePlan(req.getPlan());
            userService.updateUser(user);
        });
    }

    @Data
    static class UserReq {
        private String username;
        private User.Plan plan;
    }
}
