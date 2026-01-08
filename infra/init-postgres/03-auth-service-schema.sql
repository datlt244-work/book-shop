-- ============================================================
-- AUTH SERVICE - DATABASE SCHEMA
-- ============================================================
-- User credentials and authentication data
-- Profile data is stored in user_service_db
-- ============================================================

\c auth_service_db;

-- Create ENUM types for user roles and statuses
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
        CREATE TYPE user_role AS ENUM ('customer', 'seller', 'admin');
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_status') THEN
        CREATE TYPE user_status AS ENUM ('pending_verification', 'active', 'inactive', 'blocked');
    END IF;
END$$;

-- User Credentials Table (Authentication only)
CREATE TABLE IF NOT EXISTS user_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Authentication
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    
    -- Authorization
    role user_role NOT NULL DEFAULT 'customer',
    
    -- Account Status
    status user_status NOT NULL DEFAULT 'pending_verification',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified_at TIMESTAMP WITH TIME ZONE,
    
    -- Login Tracking
    last_login_at TIMESTAMP WITH TIME ZONE,
    last_login_ip VARCHAR(45),
    login_count INTEGER NOT NULL DEFAULT 0,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Indexes for user_credentials
CREATE INDEX IF NOT EXISTS idx_user_credentials_email ON user_credentials(email);
CREATE INDEX IF NOT EXISTS idx_user_credentials_role ON user_credentials(role);
CREATE INDEX IF NOT EXISTS idx_user_credentials_status ON user_credentials(status);
CREATE INDEX IF NOT EXISTS idx_user_credentials_created_at ON user_credentials(created_at);

-- Migration: Drop old users table if exists (WARNING: data loss!)
-- Uncomment only if you want to clean up old structure
-- DROP TABLE IF EXISTS users CASCADE;

-- Sample admin user (password: Admin@123 - bcrypt encoded)
-- INSERT INTO user_credentials (email, password_hash, role, status, email_verified)
-- VALUES ('admin@ecommerce.com', '$2a$10$...', 'admin', 'active', true);

COMMENT ON TABLE user_credentials IS 'User authentication credentials - profile data stored in user_service_db';
COMMENT ON COLUMN user_credentials.id IS 'UUID shared with user_profiles table in user_service_db';

