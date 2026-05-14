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
    'ADMIN',
    true,
    true,
    NOW()
    WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'admin@admin.com'
);