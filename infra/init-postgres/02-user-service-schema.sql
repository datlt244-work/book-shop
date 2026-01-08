-- ============================================================
-- USER SERVICE DATABASE SCHEMA
-- ============================================================
-- Service: User Profile Management
-- Database: user_service_db
-- Note: Authentication data is in auth_service_db
-- ============================================================

\c user_service_db;

-- ============================================================
-- TABLE: user_profiles
-- ============================================================
-- Stores user profile information (separate from auth credentials)
-- user_id is the same UUID as auth-service's user_credentials.id

CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY,           -- Same as auth-service user_credentials.id
    
    -- Basic Info (copied from auth for convenience)
    email VARCHAR(255) NOT NULL,        -- Copy from auth-service
    
    -- Profile Details
    full_name VARCHAR(100),
    phone_number VARCHAR(20),
    avatar_url VARCHAR(500),
    date_of_birth DATE,
    bio TEXT,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for user_profiles table
CREATE INDEX idx_user_profiles_email ON user_profiles(email);
CREATE INDEX idx_user_profiles_full_name ON user_profiles(full_name);
CREATE INDEX idx_user_profiles_created_at ON user_profiles(created_at);

-- ============================================================
-- TABLE: user_addresses
-- ============================================================
-- Stores user shipping/billing addresses

CREATE TABLE user_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_profiles(user_id) ON DELETE CASCADE,
    
    -- Recipient Info
    recipient_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    
    -- Address Details (Using province/district/ward codes for Vietnam)
    province_code VARCHAR(20) NOT NULL,
    province_name VARCHAR(100),
    district_code VARCHAR(20) NOT NULL,
    district_name VARCHAR(100),
    ward_code VARCHAR(20),
    ward_name VARCHAR(100),
    street_address TEXT NOT NULL,       -- Street, house number, etc.
    
    -- Address Type & Flags
    address_type VARCHAR(20) DEFAULT 'shipping', -- shipping, billing, both
    is_default BOOLEAN DEFAULT FALSE,
    label VARCHAR(50),                  -- Home, Office, etc.
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for user_addresses table
CREATE INDEX idx_addresses_user_id ON user_addresses(user_id);
CREATE INDEX idx_addresses_is_default ON user_addresses(is_default);
CREATE INDEX idx_addresses_province ON user_addresses(province_code);

-- ============================================================
-- TABLE: user_preferences
-- ============================================================
-- Stores user preferences and settings

CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES user_profiles(user_id) ON DELETE CASCADE,
    
    -- Notification Preferences
    email_notifications BOOLEAN DEFAULT TRUE,
    sms_notifications BOOLEAN DEFAULT FALSE,
    push_notifications BOOLEAN DEFAULT TRUE,
    
    -- Marketing Preferences
    marketing_emails BOOLEAN DEFAULT FALSE,
    newsletter BOOLEAN DEFAULT FALSE,
    
    -- Display Preferences
    language VARCHAR(10) DEFAULT 'vi',
    currency VARCHAR(10) DEFAULT 'VND',
    theme VARCHAR(20) DEFAULT 'light',
    
    -- Audit
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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

CREATE TRIGGER update_user_profiles_updated_at
    BEFORE UPDATE ON user_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_addresses_updated_at
    BEFORE UPDATE ON user_addresses
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_preferences_updated_at
    BEFORE UPDATE ON user_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- ENSURE ONLY ONE DEFAULT ADDRESS PER USER
-- ============================================================

CREATE OR REPLACE FUNCTION ensure_single_default_address()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_default = TRUE THEN
        UPDATE user_addresses 
        SET is_default = FALSE 
        WHERE user_id = NEW.user_id 
          AND id != NEW.id 
          AND is_default = TRUE;
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER ensure_single_default_address_trigger
    BEFORE INSERT OR UPDATE ON user_addresses
    FOR EACH ROW
    EXECUTE FUNCTION ensure_single_default_address();

-- ============================================================
-- COMMENTS
-- ============================================================

COMMENT ON TABLE user_profiles IS 'User profile information (separate from auth credentials)';
COMMENT ON TABLE user_addresses IS 'User shipping and billing addresses';
COMMENT ON TABLE user_preferences IS 'User notification and display preferences';
COMMENT ON COLUMN user_profiles.user_id IS 'Same UUID as auth-service user_credentials.id';
