-- ============================================================
-- Analytics Service Schema
-- Database: analytics_db
-- Eventual consistency — write-behind from Redis
-- ============================================================

USE analytics_db;

-- Daily job statistics (aggregated from events)
CREATE TABLE IF NOT EXISTS job_stats_daily (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    view_count INT NOT NULL DEFAULT 0,
    application_count INT NOT NULL DEFAULT 0,
    unique_viewers INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- One row per job per day
    CONSTRAINT uk_job_stats_daily UNIQUE (job_id, stat_date),

    INDEX idx_job_stats_job (job_id),
    INDEX idx_job_stats_date (stat_date),
    INDEX idx_job_stats_job_date (job_id, stat_date DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Application metrics (aggregated counters)
CREATE TABLE IF NOT EXISTS application_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_date DATE NOT NULL,
    total_applications INT NOT NULL DEFAULT 0,
    total_accepted INT NOT NULL DEFAULT 0,
    total_rejected INT NOT NULL DEFAULT 0,
    avg_review_time_hours DECIMAL(10, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_application_metrics_date UNIQUE (metric_date),
    INDEX idx_application_metrics_date (metric_date DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User activity tracking
CREATE TABLE IF NOT EXISTS user_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_type ENUM('JOB_VIEW', 'JOB_SEARCH', 'APPLICATION_SUBMIT', 'PROFILE_UPDATE') NOT NULL,
    metadata JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_activity_user (user_id),
    INDEX idx_user_activity_type (activity_type),
    INDEX idx_user_activity_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
