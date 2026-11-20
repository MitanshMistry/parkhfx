-- V2: emails are unique ignoring case ("Ada@x.com" and "ada@x.com" are the same account).
-- The app also lowercases emails, this index is the safety net.
CREATE UNIQUE INDEX users_email_lower_key ON users (lower(email));
