package org.bitmagic.ifeed.domain.service;

import org.bitmagic.ifeed.domain.model.User;

public record AuthToken(User user, String token) {
}
