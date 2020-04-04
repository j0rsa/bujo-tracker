ALTER SYSTEM SET max_connections = 1000;
ALTER SYSTEM SET wal_level = minimal;
ALTER SYSTEM SET commit_delay = 100;
SET TIME ZONE 'UTC';
create extension if not exists "uuid-ossp";