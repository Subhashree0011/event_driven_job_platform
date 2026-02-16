-- ============================================================
-- Job Service Schema
-- Database: job_db
-- ============================================================

USE job_db;

-- Companies table
CREATE TABLE IF NOT EXISTS companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    website VARCHAR(500),
    logo_url VARCHAR(500),
    industry VARCHAR(100),
    company_size ENUM('STARTUP', 'SMALL', 'MEDIUM', 'LARGE', 'ENTERPRISE') DEFAULT 'MEDIUM',
    location VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_companies_name (name),
    INDEX idx_companies_industry (industry)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Jobs table
CREATE TABLE IF NOT EXISTS jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    employer_id BIGINT NOT NULL,          -- references user_id from user_db (application-level integrity)
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    requirements TEXT,
    role VARCHAR(100) NOT NULL,            -- e.g., "Backend Engineer", "Frontend Developer"
    location VARCHAR(255) NOT NULL,
    job_type ENUM('FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP', 'REMOTE') NOT NULL DEFAULT 'FULL_TIME',
    experience_level ENUM('ENTRY', 'MID', 'SENIOR', 'LEAD', 'PRINCIPAL') NOT NULL DEFAULT 'MID',
    salary_min DECIMAL(12, 2),
    salary_max DECIMAL(12, 2),
    currency VARCHAR(3) DEFAULT 'USD',
    status ENUM('DRAFT', 'ACTIVE', 'PAUSED', 'CLOSED', 'EXPIRED') NOT NULL DEFAULT 'DRAFT',
    application_deadline DATE,
    view_count INT NOT NULL DEFAULT 0,
    application_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_jobs_company FOREIGN KEY (company_id) REFERENCES companies(id),

    -- Composite index for search queries (key pattern from ARCHITECTURE.md)
    -- Order: low cardinality first → selective → sort column
    INDEX idx_job_search (status, role, location, created_at DESC),
    INDEX idx_jobs_company (company_id),
    INDEX idx_jobs_employer (employer_id),
    INDEX idx_jobs_type_level (job_type, experience_level),
    INDEX idx_jobs_status_created (status, created_at DESC),
    INDEX idx_jobs_salary (salary_min, salary_max)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Job skills (many-to-many via join table)
CREATE TABLE IF NOT EXISTS job_skills (
    job_id BIGINT NOT NULL,
    skill VARCHAR(100) NOT NULL,
    PRIMARY KEY (job_id, skill),
    CONSTRAINT fk_job_skills_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    INDEX idx_job_skills_skill (skill)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
