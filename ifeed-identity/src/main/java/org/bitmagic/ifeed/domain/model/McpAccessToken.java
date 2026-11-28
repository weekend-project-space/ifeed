package org.bitmagic.ifeed.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mcp_access_tokens")
public class McpAccessToken {
    @Id
    @Column(name = "token_id", nullable = false, updatable = false)
    private UUID tokenId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected McpAccessToken() { }

    public McpAccessToken(UUID tokenId, User user, String name, Instant expiresAt) {
        this.tokenId = tokenId;
        this.user = user;
        this.name = name;
        this.expiresAt = expiresAt;
    }

    public UUID getTokenId() { return tokenId; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
