import apiClient from './apiClient';
import type {
  Job,
  CreateJobRequest,
  UpdateJobRequest,
  JobSearchParams,
  PageResponse,
  Company,
  CreateCompanyRequest,
} from '@/types';
import { tokenManager } from '@/security/tokenManager';

const JOBS_BASE = '/api/v1/jobs';
const COMPANIES_BASE = '/api/v1/jobs/companies';

export const jobService = {
  // ─── Jobs ────────────────────────────────────────────
  async searchJobs(params: JobSearchParams): Promise<PageResponse<Job>> {
    const { data } = await apiClient.get<PageResponse<Job>>(`${JOBS_BASE}/search`, { params });
    return data;
  },

  async getJob(id: number): Promise<Job> {
    const { data } = await apiClient.get<Job>(`${JOBS_BASE}/${id}`);
    return data;
  },

  async getJobsByEmployer(employerId: number, page = 0, size = 20): Promise<PageResponse<Job>> {
    const { data } = await apiClient.get<PageResponse<Job>>(`${JOBS_BASE}/employer/${employerId}`, {
      params: { page, size },
    });
    return data;
  },

  async getJobsByCompany(companyId: number, page = 0, size = 20): Promise<PageResponse<Job>> {
    const { data } = await apiClient.get<PageResponse<Job>>(`${JOBS_BASE}/company/${companyId}`, {
      params: { page, size },
    });
    return data;
  },

  async createJob(jobData: CreateJobRequest): Promise<Job> {
    const { data } = await apiClient.post<Job>(JOBS_BASE, jobData);
    return data;
  },

  async updateJob(id: number, jobData: UpdateJobRequest): Promise<Job> {
    const { data } = await apiClient.put<Job>(`${JOBS_BASE}/${id}`, jobData);
    return data;
  },

  async activateJob(id: number): Promise<Job> {
    const { data } = await apiClient.put<Job>(`${JOBS_BASE}/${id}/activate`);
    return data;
  },

  async pauseJob(id: number): Promise<Job> {
    const { data } = await apiClient.put<Job>(`${JOBS_BASE}/${id}/pause`);
    return data;
  },

  async closeJob(id: number): Promise<Job> {
    const { data } = await apiClient.put<Job>(`${JOBS_BASE}/${id}/close`);
    return data;
  },

  async deleteJob(id: number): Promise<void> {
    await apiClient.delete(`${JOBS_BASE}/${id}`);
  },

  /** Fetches jobs owned by the current user via the employer endpoint */
  async getMyJobs(): Promise<Job[]> {
    const userId = tokenManager.getUserId();
    if (!userId) return [];
    const { data } = await apiClient.get<PageResponse<Job>>(`${JOBS_BASE}/employer/${userId}`, {
      params: { page: 0, size: 100 },
    });
    return data.content;
  },

  // ─── Companies ───────────────────────────────────────
  async getAllCompanies(): Promise<Company[]> {
    const { data } = await apiClient.get<Company[]>(COMPANIES_BASE);
    return data;
  },

  async getMyCompanies(): Promise<Company[]> {
    const { data } = await apiClient.get<Company[]>(`${COMPANIES_BASE}/mine`);
    return data;
  },

  async createCompany(companyData: CreateCompanyRequest): Promise<Company> {
    const { data } = await apiClient.post<Company>(COMPANIES_BASE, companyData);
    return data;
  },
};
