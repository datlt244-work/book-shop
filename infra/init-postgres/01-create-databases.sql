-- ============================================================
-- ECOMMERCE SYSTEM - DATABASE INITIALIZATION
-- ============================================================
-- This script creates all required databases for microservices
-- Each service has its own isolated database (Database per Service pattern)
-- ============================================================

-- Create databases for each microservice
CREATE DATABASE user_service_db;
CREATE DATABASE inventory_service_db;
CREATE DATABASE order_service_db;
CREATE DATABASE payment_service_db;

-- Grant privileges (optional, for specific users)
-- GRANT ALL PRIVILEGES ON DATABASE user_service_db TO ecom_user;
-- GRANT ALL PRIVILEGES ON DATABASE inventory_service_db TO ecom_user;
-- GRANT ALL PRIVILEGES ON DATABASE order_service_db TO ecom_user;
-- GRANT ALL PRIVILEGES ON DATABASE payment_service_db TO ecom_user;
