-- Run once as a MySQL administrator:   mysql -u root -p < scripts/setup-database.sql
-- Replace CHANGE_ME with a strong password and put the same value in your .env (DB_PASSWORD / TEST_DB_PASSWORD).
CREATE DATABASE IF NOT EXISTS ecoswap_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS ecoswap_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'ecoswap'@'localhost' IDENTIFIED BY 'CHANGE_ME';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES ON ecoswap_db.*   TO 'ecoswap'@'localhost';
GRANT ALL PRIVILEGES ON ecoswap_test.* TO 'ecoswap'@'localhost';
FLUSH PRIVILEGES;
-- Tables are created automatically by Flyway the first time the application starts.
