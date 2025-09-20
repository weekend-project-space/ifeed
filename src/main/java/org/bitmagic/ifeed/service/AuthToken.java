package org.bitmagic.ifeed.service;

import org.bitmagic.ifeed.domain.entity.User;

public record AuthToken(User user, String token) {
}
