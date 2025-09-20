package org.bitmagic.ifeed.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(String token, @JsonProperty("user_id") String userId) {
}
