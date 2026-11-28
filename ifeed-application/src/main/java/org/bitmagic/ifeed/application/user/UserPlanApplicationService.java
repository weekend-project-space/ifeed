package org.bitmagic.ifeed.application.user;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.repository.UserRepository;
import org.bitmagic.ifeed.domain.service.UserService;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserPlanApplicationService {
    private final UserRepository users;
    private final UserService userService;

    public void updatePlan(String username, User.Plan plan) {
        users.findByUsername(username).ifPresent(user -> {
            user.changePlan(plan);
            userService.updateUser(user);
        });
    }
}
