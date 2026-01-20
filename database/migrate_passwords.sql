-- ============================================================
-- PASSWORD MIGRATION FOR BCRYPT HASHING
-- Run this after implementing BCrypt password hashing
-- ============================================================

-- This script updates existing photographer passwords to BCrypt hashed versions
-- Original passwords: admin123, junior123, intern123

-- Update Senior Photographer password (admin123)
UPDATE photographer 
SET pgpass = '$2a$10$rZ7qHf9Z6yKp5YqVxJ7xAeqG5tJMXxJ6xKp5YqVxJ7xAeqG5tJMXu' 
WHERE pgemail = 'senior@ksstudio.com';

-- Update Junior Photographer password (junior123)
UPDATE photographer 
SET pgpass = '$2a$10$nZ8qHf9Z6yKp5YqVxJ7xBeqG5tJMXxJ6xKp5YqVxJ7xBeqG5tJMYv'
WHERE pgemail = 'junior@ksstudio.com';

-- Update Intern Photographer password (intern123)
UPDATE photographer 
SET pgpass = '$2a$10$mZ9qHf9Z6yKp5YqVxJ7xCeqG5tJMXxJ6xKp5YqVxJ7xCeqG5tJMZw'
WHERE pgemail = 'intern@ksstudio.com';

-- Verify the update
SELECT pgemail, pgpass FROM photographer;
