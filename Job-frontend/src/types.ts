// ─── User & Auth ───────────────────────────────────────────
export type UserRole = 'JOB_SEEKER' | 'EMPLOYER';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: UserRole;
  status?: string;
  emailVerified?: boolean;
  profilePictureUrl?: string;
  bio?: string;
  resumeUrl?: string;
  location?: string;
  createdAt?: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresIn?: number;
  user?: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role?: UserRole;
}

export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  profilePictureUrl?: string;
  bio?: string;
  resumeUrl?: string;
  location?: string;
}

// ─── Company ───────────────────────────────────────────────
export type CompanySize = 'STARTUP' | 'SMALL' | 'MEDIUM' | 'LARGE' | 'ENTERPRISE';

export interface Company {
  id: number;
  name: string;
  description?: string;
  website?: string;
  logoUrl?: string;
  industry?: string;
  companySize?: CompanySize;
  location?: string;
  createdBy: number;
  createdAt?: string;
}

export interface CreateCompanyRequest {
  name: string;
  description?: string;
  website?: string;
  logoUrl?: string;
  industry?: string;
  companySize?: CompanySize;
  location?: string;
}

// ─── Job ───────────────────────────────────────────────────
export type JobStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'CLOSED';
export type JobType = 'FULL_TIME' | 'PART_TIME' | 'CONTRACT' | 'INTERNSHIP' | 'FREELANCE';
export type ExperienceLevel = 'ENTRY' | 'JUNIOR' | 'MID' | 'SENIOR' | 'LEAD';

export interface CompanyInfo {
  id: number;
  name: string;
  description?: string;
  website?: string;
  logoUrl?: string;
  industry?: string;
  companySize?: CompanySize;
  location?: string;
  createdBy?: number;
  createdAt?: string;
}

export interface Job {
  id: number;
  company?: CompanyInfo;
  employerId: number;
  title: string;
  description: string;
  requirements?: string;
  role: string;
  location: string;
  jobType: JobType;
  experienceLevel: ExperienceLevel;
  salaryMin?: number;
  salaryMax?: number;
  currency?: string;
  status: JobStatus;
  applicationDeadline?: string;
  viewCount?: number;
  applicationCount?: number;
  skills: string[];
  createdAt: string;
  updatedAt?: string;
}

export interface CreateJobRequest {
  companyId: number;
  title: string;
  description: string;
  requirements?: string;
  role: string;
  location: string;
  jobType?: JobType;
  experienceLevel?: ExperienceLevel;
  salaryMin?: number;
  salaryMax?: number;
  currency?: string;
  applicationDeadline?: string;
  skills?: string[];
}

export interface UpdateJobRequest extends Partial<CreateJobRequest> {}

export interface JobSearchParams {
  keyword?: string;
  location?: string;
  role?: string;
  jobType?: JobType;
  experienceLevel?: ExperienceLevel;
  status?: JobStatus;
  companyId?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  last: boolean;
}

// ─── Application ───────────────────────────────────────────
export type ApplicationStatus =
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'SHORTLISTED'
  | 'INTERVIEW'
  | 'OFFERED'
  | 'REJECTED'
  | 'WITHDRAWN';

export interface Application {
  id: number;
  jobId: number;
  userId: number;
  status: ApplicationStatus;
  coverLetter?: string;
  resumeUrl?: string;
  notes?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface ApplyRequest {
  jobId: number;
  coverLetter?: string;
  resumeUrl?: string;
}

// ─── Toast / UI ────────────────────────────────────────────
export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: string;
  type: ToastType;
  message: string;
  duration?: number;
}

// ─── Load Testing ──────────────────────────────────────────
export interface LoadTestConfig {
  totalRequests: number;
  concurrency: number;
  bypassRateLimit: boolean;
}

export interface LoadTestResult {
  id: string;
  jobId: number;
  jobTitle: string;
  config: LoadTestConfig;
  timestamp: string;
  duration: number;
  totalRequests: number;
  successCount: number;
  failureCount: number;
  avgLatency: number;
  minLatency: number;
  maxLatency: number;
  p95Latency: number;
  p99Latency: number;
  throughput: number;
  rateLimitedCount: number;
  statusCodes: Record<string, number>;
  errors: string[];
  pipeline: {
    kafka: { published: number; failed: number };
    database: { saved: number; failed: number };
    redis: { hits: number; misses: number };
  };
}
