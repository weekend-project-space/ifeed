package org.bitmagic.ifeed.domain.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.domain.model.McpAccessToken;
import org.bitmagic.ifeed.domain.model.User;
import org.bitmagic.ifeed.domain.repository.McpAccessTokenRepository;
import org.bitmagic.ifeed.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtTokenService {
    private static final String ISSUER = "ifeed";
    private static final String AUDIENCE = "ifeed-api";
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final UserRepository users;
    private final McpAccessTokenRepository mcpTokens;
    @Value("${app.jwt.web-ttl:PT12H}") private Duration webTtl;
    @Value("${app.jwt.mcp-ttl:P30D}") private Duration mcpTtl;

    public static SecretKey secretKey(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalStateException("APP_JWT_SECRET must contain at least 32 UTF-8 bytes");
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String issueWeb(User user) {
        return encode(user, "WEB", UUID.randomUUID(), Instant.now().plus(webTtl));
    }

    @Transactional
    public IssuedMcpToken issueMcp(Integer userId, String name) {
        if (name == null || name.isBlank() || name.trim().length() > 64)
            throw new IllegalArgumentException("名称不能为空，最多 64 个字符");
        var user = users.findById(userId).orElseThrow(() -> new BadCredentialsException("Invalid user"));
        var id = UUID.randomUUID();
        var expiresAt = Instant.now().plus(mcpTtl);
        mcpTokens.deleteByExpiresAtBefore(Instant.now());
        mcpTokens.save(new McpAccessToken(id, user, name.trim(), expiresAt));
        return new IssuedMcpToken(id, name.trim(), encode(user, "MCP", id, expiresAt), expiresAt);
    }

    public Identity authenticate(String rawToken) {
        try {
            Jwt jwt = decoder.decode(rawToken);
            Integer userId = Integer.valueOf(jwt.getSubject());
            User user = users.findById(userId).orElseThrow(() -> new BadCredentialsException("Invalid token"));
            if ("MCP".equals(jwt.getClaimAsString("kind"))) {
                UUID id = UUID.fromString(jwt.getId());
                var record = mcpTokens.findByTokenIdAndUser_IdAndExpiresAtAfter(id, userId, Instant.now())
                        .orElseThrow(() -> new BadCredentialsException("Invalid token"));
                if (!record.getTokenId().equals(id))
                    throw new BadCredentialsException("Invalid token");
                return new Identity(user, true);
            }
            if (!"WEB".equals(jwt.getClaimAsString("kind"))) throw new BadCredentialsException("Invalid token");
            return new Identity(user, false);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BadCredentialsException("Invalid token", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<McpAccessToken> listMcp(Integer userId) {
        return mcpTokens.findAllByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void revokeMcp(Integer userId, UUID id) {
        var token = mcpTokens.findByTokenIdAndUser_IdAndExpiresAtAfter(id, userId, Instant.now())
                .orElseThrow(() -> new IllegalArgumentException("凭证不存在"));
        mcpTokens.delete(token);
    }

    private String encode(User user, String kind, UUID id, Instant expiresAt) {
        var claims = JwtClaimsSet.builder().issuer(ISSUER).audience(List.of(AUDIENCE))
                .subject(user.getId().toString()).issuedAt(Instant.now()).expiresAt(expiresAt)
                .id(id.toString()).claim("kind", kind).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public record Identity(User user, boolean mcp) { }
    public record IssuedMcpToken(UUID id, String name, String token, Instant expiresAt) { }
}
