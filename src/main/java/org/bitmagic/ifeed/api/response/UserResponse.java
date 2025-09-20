package org.bitmagic.ifeed.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserResponse(@JsonProperty("user_id") String userId, String username) {
}
