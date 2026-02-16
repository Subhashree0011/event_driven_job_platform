-- ============================================================
-- Application Service Schema
-- Database: application_db
-- Uses READ COMMITTED isolation (set at service level)
-- ============================================================

USE application_db;

-- Applications table
CREATE TABLE IF NOT EXISTS applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,                -- references job_id from job_db (application-level integrity)
    user_id BIGINT NOT NULL,               -- references user_id from user_db (application-level integrity)
    status ENUM('SUBMITTED', 'UNDER_REVIEW', 'SHORTLISTED', 'INTERVIEW', 'OFFERED', 'REJECTED', 'WITHDRAWN') NOT NULL DEFAULT 'SUBMITTED',
    cover_letter TEXT,
    resume_url VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Prevent duplicate applications: one user per job
    CONSTRAINT uk_application_user_job UNIQUE (user_id, job_id),

    INDEX idx_applications_job (job_id),
    INDEX idx_applications_user (user_id),
    INDEX idx_applications_status (status),
    INDEX idx_applications_job_status (job_id, status),
    INDEX idx_applications_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Outbox events table (transactional outbox pattern)
-- Events are written in the same transaction as the application
-- An async poller reads unpublished events and publishes to Kafka
CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,    -- e.g., "Application"
    aggregate_id BIGINT NOT NULL,            -- e.g., application_id
    event_type VARCHAR(100) NOT NULL,        -- e.g., "APPLICATION_CREATED"
    payload JSON NOT NULL,                    -- JSON event payload
    topic VARCHAR(255) NOT NULL,             -- Kafka topic name
    partition_key VARCHAR(255),              -- Kafka partition key (job_id)
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_outbox_unpublished (published, created_at),
    INDEX idx_outbox_aggregate (aggregate_type, aggregate_id),
    INDEX idx_outbox_retry (published, retry_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
