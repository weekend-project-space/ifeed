package org.bitmagic.ifeed.config.security;

import org.bitmagic.ifeed.domain.service.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtConfigurationTest {
    private final JwtConfiguration configuration = new JwtConfiguration();

    @Test
    void acceptsOnlyTokensWithExpectedIssuerAndAudience() {
        var key = configuration.jwtSecretKey("01234567890123456789012345678901");
        var encoder = configuration.jwtEncoder(key);
        var decoder = configuration.jwtDecoder(key);
        var valid = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims("ifeed", List.of("ifeed-api"))));
        assertEquals("ifeed", decoder.decode(valid.getTokenValue()).getClaimAsString("iss"));

        var badAudience = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims("ifeed", List.of("other"))));
        assertThrows(JwtException.class, () -> decoder.decode(badAudience.getTokenValue()));

        var badIssuer = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims("other", List.of("ifeed-api"))));
        assertThrows(JwtException.class, () -> decoder.decode(badIssuer.getTokenValue()));
    }

    @Test
    void rejectsWeakSecrets() {
        assertThrows(IllegalStateException.class, () -> JwtTokenService.secretKey("too-short"));
    }

    private static JwtClaimsSet claims(String issuer, List<String> audience) {
        var now = Instant.now();
        return JwtClaimsSet.builder().issuer(issuer).audience(audience).subject("1")
                .issuedAt(now).expiresAt(now.plusSeconds(60)).build();
    }
}
