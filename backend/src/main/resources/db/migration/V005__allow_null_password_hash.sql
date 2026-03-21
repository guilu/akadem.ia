-- OAuth2 users authenticate via Google and have no local password.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
