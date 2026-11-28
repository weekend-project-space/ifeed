CREATE TABLE mcp_access_tokens (
    token_id UUID PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mcp_access_tokens_user_id ON mcp_access_tokens(user_id);
CREATE INDEX idx_mcp_access_tokens_expires_at ON mcp_access_tokens(expires_at);
