-- MediTracker
-- Flyway migration V3: email verification and password reset

ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Accounts that existed before verification was introduced are trusted
UPDATE users SET email_verified = TRUE;

-- One-time tokens sent by email. Only the SHA-256 hash of the token is stored.
CREATE TABLE IF NOT EXISTS auth_token (
    token_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auth_token_user_type ON auth_token(user_id, type);
