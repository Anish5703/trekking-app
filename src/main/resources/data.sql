CREATE INDEX IF NOT EXISTS idx_routes_path ON routes USING GIST(path);


ALTER TABLE users DROP CONSTRAINT users_role_check;

ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('ADMIN', 'CUSTOMER', 'SUPERADMIN'));



-- Insert only if not exists (seed), directly as SUPERADMIN
INSERT INTO users (
    name,
    email,
    password,
    contact,
    role,
    email_verified,
    is_active,
    time_stamp
)
SELECT
    'super admin',
    'admin@admin.com',
    '$2a$10$3pmBQT1IsrI2UNDVPekwaeQQZYcnHpL.JFEbBWBeOpXHfOuD5Zwpu',
    '9857099999',
    'SUPERADMIN',
    true,
    true,
    NOW()
    WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'admin@admin.com'
);
-- Update existing admin@admin.com user to SUPERADMIN if currently ADMIN
UPDATE users
SET role = 'SUPERADMIN'
WHERE email = 'admin@admin.com'
  AND role = 'ADMIN';