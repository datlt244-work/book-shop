-- ============================================================
-- AUTH SERVICE DATABASE SCHEMA
-- ============================================================
-- Service: Authentication & Authorization
-- Database: auth_service_db
-- Note: User profiles are in user_service_db
-- ============================================================

\c auth_service_db;

-- ============================================================
-- ENUM TYPES
-- ============================================================

CREATE TYPE user_role AS ENUM ('customer', 'admin');
CREATE TYPE user_status AS ENUM ('active', 'inactive', 'blocked', 'pending_verification');

-- ============================================================
-- TABLE: user_credentials
-- ============================================================
-- Stores user authentication credentials only

CREATE TABLE user_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Authentication
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    
    -- Authorization
    role user_role DEFAULT 'customer',
    
    -- Account Status
    status user_status DEFAULT 'pending_verification',
    email_verified BOOLEAN DEFAULT FALSE,
    email_verified_at TIMESTAMP,
    
    -- Login Tracking
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(45),          -- Support IPv6
    login_count INT DEFAULT 0,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for user_credentials table
CREATE INDEX idx_user_credentials_email ON user_credentials(email);
CREATE INDEX idx_user_credentials_role ON user_credentials(role);
CREATE INDEX idx_user_credentials_status ON user_credentials(status);
CREATE INDEX idx_user_credentials_created_at ON user_credentials(created_at);

-- ============================================================
-- TABLE: oauth2_linked_accounts (for Phase 3)
-- ============================================================
-- Stores OAuth2 provider linked accounts

CREATE TABLE oauth2_linked_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_credentials(id) ON DELETE CASCADE,
    
    -- Provider Info
    provider VARCHAR(50) NOT NULL,      -- google, facebook, github, apple
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    
    -- Token Storage (encrypted)
    access_token TEXT,
    refresh_token TEXT,
    token_expires_at TIMESTAMP,
    
    -- Audit
    linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    
    UNIQUE(provider, provider_user_id),
    UNIQUE(user_id, provider)
);

CREATE INDEX idx_oauth2_user_id ON oauth2_linked_accounts(user_id);
CREATE INDEX idx_oauth2_provider ON oauth2_linked_accounts(provider);

-- ============================================================
-- TABLE: password_reset_tokens
-- ============================================================

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_credentials(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_password_reset_user ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_expires ON password_reset_tokens(expires_at);

-- ============================================================
-- TABLE: email_verification_tokens
-- ============================================================

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_credentials(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_verify_user ON email_verification_tokens(user_id);
CREATE INDEX idx_email_verify_token ON email_verification_tokens(token);

-- ============================================================
-- TRIGGER: Auto-update updated_at
-- ============================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_user_credentials_updated_at
    BEFORE UPDATE ON user_credentials
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- COMMENTS
-- ============================================================

COMMENT ON TABLE user_credentials IS 'User authentication credentials (password, status, role)';
COMMENT ON TABLE oauth2_linked_accounts IS 'OAuth2 provider linked accounts (Google, Facebook, etc.)';
COMMENT ON TABLE password_reset_tokens IS 'Password reset request tokens';
COMMENT ON TABLE email_verification_tokens IS 'Email verification tokens';
COMMENT ON COLUMN user_credentials.id IS 'UUID shared with user_service user_profiles.user_id';

