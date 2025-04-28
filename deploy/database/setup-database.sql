-- Create user if it doesn't exist and update password
DO
$do$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'middleware_user') THEN
      CREATE USER middleware_user WITH PASSWORD 'secure_password';
   ELSE
      ALTER USER middleware_user WITH PASSWORD 'secure_password';
   END IF;
END
$do$;

-- Grant privileges (will work even if already granted)
GRANT ALL PRIVILEGES ON DATABASE middleware_config TO middleware_user;

-- Connect to the database
\c middleware_config middleware_user

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO middleware_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO middleware_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO middleware_user;

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Update the code field constraint (if table exists)
DO
$do$
BEGIN
   IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'clients') THEN
      ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_code_check;
   END IF;
END
$do$; 