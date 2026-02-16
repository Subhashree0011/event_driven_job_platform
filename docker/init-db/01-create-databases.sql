-- ============================================================
-- Database initialization script
-- Creates separate databases for each microservice
-- ============================================================

-- User Service Database
CREATE DATABASE IF NOT EXISTS user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON user_db.* TO 'platform'@'%';

-- Job Service Database
CREATE DATABASE IF NOT EXISTS job_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON job_db.* TO 'platform'@'%';

-- Application Service Database
CREATE DATABASE IF NOT EXISTS application_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON application_db.* TO 'platform'@'%';

-- Analytics Service Database
CREATE DATABASE IF NOT EXISTS analytics_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON analytics_db.* TO 'platform'@'%';

FLUSH PRIVILEGES;
