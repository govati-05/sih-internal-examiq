-- Create admin user with specified credentials
-- Username: Durga prasad
-- Email: durgaprasadkalavalapalli756@gmail.com
-- Full name: Durga prasad
-- Password: admin123 (change this after first login)

INSERT INTO users (username, email, password, full_name, role_id, status)
VALUES (
    'Durga prasad',
    'durgaprasadkalavalapalli756@gmail.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', -- BCrypt hash for 'admin123'
    'Durga prasad',
    (SELECT id FROM roles WHERE name = 'ADMIN'),
    'ACTIVE'
);
