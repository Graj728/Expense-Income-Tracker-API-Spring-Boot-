-- Tracks issued refresh tokens so they can be individually revoked (logout,
-- password change, suspected compromise) instead of remaining valid for their
-- full lifetime with no way to invalidate them server-side.
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_id VARCHAR(64) NOT NULL UNIQUE,   -- the JWT's "jti" claim, not the token itself
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_token_id ON refresh_tokens(token_id);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
