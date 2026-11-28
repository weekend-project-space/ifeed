package org.bitmagic.ifeed.mcp;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.security.UserPrincipal;
import org.bitmagic.ifeed.domain.service.JwtTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/mcp-tokens")
@RequiredArgsConstructor
public class McpTokenController {
    private final JwtTokenService tokens;

    @GetMapping
    public List<TokenInfo> list(@AuthenticationPrincipal UserPrincipal principal) {
        return tokens.listMcp(principal.getId()).stream()
                .map(token -> new TokenInfo(token.getTokenId(), token.getName(), token.getCreatedAt(), token.getExpiresAt()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IssuedToken create(@AuthenticationPrincipal UserPrincipal principal, @RequestBody CreateTokenRequest request) {
        try {
            var issued = tokens.issueMcp(principal.getId(), request.name());
            return new IssuedToken(issued.id(), issued.name(), issued.token(), issued.expiresAt());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        try {
            tokens.revokeMcp(principal.getId(), id);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
        }
    }

    public record CreateTokenRequest(String name) { }
    public record TokenInfo(UUID id, String name, Instant createdAt, Instant expiresAt) { }
    public record IssuedToken(UUID id, String name, String token, Instant expiresAt) { }
}
