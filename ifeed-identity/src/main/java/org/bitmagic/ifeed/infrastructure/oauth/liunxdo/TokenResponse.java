package org.bitmagic.ifeed.infrastructure.oauth.liunxdo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author yangrd
 * @date 2025/11/27
 **/
@Data
public class TokenResponse {

    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("refresh_token")
    private String refreshToken;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("expires_in")
    private Integer expiresIn;
}
