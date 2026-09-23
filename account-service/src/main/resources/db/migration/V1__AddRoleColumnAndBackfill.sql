-- Add role column if it doesn't exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(50);

-- Backfill existing users with default role PASSENGER
UPDATE users SET role = 'PASSENGER' WHERE role IS NULL;

-- Add NOT NULL constraint
ALTER TABLE users ALTER COLUMN role SET NOT NULL;

-- Set default for future inserts
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'PASSENGER';

