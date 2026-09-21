-- Add role column to users table for RBAC support
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Set existing admin user
UPDATE users SET role = 'ADMIN' WHERE username = 'yangrd';

-- Index for role-based queries
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
