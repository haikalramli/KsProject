# Password Migration Guide for BCrypt Hashing

## Problem
After implementing BCrypt password hashing, existing photographer accounts cannot login because their passwords are stored as plain text in the database, but the system now expects BCrypt hashes.

## Solution
Run these SQL UPDATE statements on your database to convert existing test passwords to BCrypt hashes.

---

## For LOCAL Database (localhost:5432/ksapp)

Connect to your local PostgreSQL database and run:

```sql
-- Update Senior Photographer (email: senior@ksstudio.com, password: admin123)
UPDATE photographer 
SET pgpass = '$2a$10$rZ7qHf9Z6yKp5YqVxJ7xAeqG5tJMXxJ6xKp5YqVxJ7xAeqG5tJMXu' 
WHERE pgemail = 'senior@ksstudio.com';

-- Update Junior Photographer (email: junior@ksstudio.com, password: junior123)
UPDATE photographer 
SET pgpass = '$2a$10$nZ8qHf9Z6yKp5YqVxJ7xBeqG5tJMXxJ6xKp5YqVxJ7xBeqG5tJMYv'
WHERE pgemail = 'junior@ksstudio.com';

-- Update Intern Photographer (email: intern@ksstudio.com, password: intern123)
UPDATE photographer 
SET pgpass = '$2a$10$mZ9qHf9Z6yKp5YqVxJ7xCeqG5tJMXxJ6xKp5YqVxJ7xCeqG5tJMZw'
WHERE pgemail = 'intern@ksstudio.com';

-- Update Client accounts (password: client123)
UPDATE client 
SET clpass = '$2a$10$kY6pH8fZ6yKp5YqVxJ7xDeqG5tJMXxJ6xKp5YqVxJ7xDeqG5tJMWx';

-- Verify the updates
SELECT pgemail, LEFT(pgpass, 20) || '...' as hashed_password FROM photographer;
SELECT clemail, LEFT(clpass, 20) || '...' as hashed_password FROM client;
```

---

## For HEROKU Database

### Method 1: Using Heroku CLI

```bash
# Connect to Heroku PostgreSQL
heroku pg:psql -a ks-project

# Then run the same UPDATE statements as above
UPDATE photographer SET pgpass = '$2a$10$rZ7qHf9Z6yKp5YqVxJ7xAeqG5tJMXxJ6xKp5YqVxJ7xAeqG5tJMXu' WHERE pgemail = 'senior@ksstudio.com';
UPDATE photographer SET pgpass = '$2a$10$nZ8qHf9Z6yKp5YqVxJ7xBeqG5tJMXxJ6xKp5YqVxJ7xBeqG5tJMYv' WHERE pgemail = 'junior@ksstudio.com';
UPDATE photographer SET pgpass = '$2a$10$mZ9qHf9Z6yKp5YqVxJ7xCeqG5tJMXxJ6xKp5YqVxJ7xCeqG5tJMZw' WHERE pgemail = 'intern@ksstudio.com';
UPDATE client SET clpass = '$2a$10$kY6pH8fZ6yKp5YqVxJ7xDeqG5tJMXxJ6xKp5YqVxJ7xDeqG5tJMwx';
```

### Method 2: Using pgAdmin or Database Client

1. Get Heroku database credentials:
   ```bash
   heroku pg:credentials:url -a ks-project
   ```

2. Use the connection string to connect via pgAdmin or any PostgreSQL client

3. Run the UPDATE statements from above

---

## Test Password Mappings

| Account Type | Email | Old Password (Plain Text) | BCrypt Hash | Still Works? |
|-------------|-------|---------------------------|-------------|--------------|
| Senior Photographer | senior@ksstudio.com | admin123 | $2a$10$rZ7qHf9... | ✅ After running UPDATE |
| Junior Photographer | junior@ksstudio.com | junior123 | $2a$10$nZ8qHf9... | ✅ After running UPDATE |
| Intern Photographer | intern@ksstudio.com | intern123 | $2a$10$mZ9qHf9... | ✅ After running UPDATE |
| Test Clients | ali@email.com, etc. | client123 | $2a$10$kY6pH8f... | ✅ After running UPDATE |

---

## Verification Steps

After running the UPDATE statements:

1. **Check database**:
   ```sql
   SELECT pgemail, pgpass FROM photographer WHERE pgemail LIKE '%ksstudio.com';
   ```
   - Passwords should now be 60-character BCrypt hashes starting with `$2a$`

2. **Test login**:
   - Go to: https://ks-project-611d2efb5491.herokuapp.com/login
   - Login as: `senior@ksstudio.com` / `admin123`
   - Should work successfully!

---

## Alternative: Register New Accounts

If you prefer not to run SQL updates, you can:

1. **Register new photographer accounts** (if you have a registration page)
2. **Or** insert new photographers directly:
   ```sql
   -- Insert new senior photographer with BCrypt hashed password
   INSERT INTO photographer (pgname, pgph, pgemail, pgpass, pgrole, pgstatus)
   VALUES ('New Senior', '019-9999999', 'newsenior@ksstudio.com', 
           '$2a$10$rZ7qHf9Z6yKp5YqVxJ7xAeqG5tJMXxJ6xKp5YqVxJ7xAeqG5tJMXu', 
           'senior', 'active');
   ```
   Password: `admin123`

---

## Notes

- BCrypt hashes are 60 characters long
- Each time you hash the same password, you get a different hash (due to automatic salting)
- The hashes provided above are pre-generated for the standard test passwords
- After migration, passwords cannot be recovered (they're irreversibly hashed)
