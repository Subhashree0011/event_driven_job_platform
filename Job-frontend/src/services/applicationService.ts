import apiClient from './apiClient';
import type { Application, ApplyRequest, ApplicationStatus, PageResponse } from '@/types';
import { tokenManager } from '@/security/tokenManager';

const BASE = '/api/v1/applications';

export const applicationService = {
  async apply(applyData: ApplyRequest): Promise<Application> {
    const { data } = await apiClient.post<Application>(BASE, applyData);
    return data;
  },

  async getApplication(id: number): Promise<Application> {
    const { data } = await apiClient.get<Application>(`${BASE}/${id}`);
    return data;
  },

  /** Fetches current user's applications via /user/{userId} endpoint */
  async getMyApplications(page = 0, size = 50): Promise<Application[]> {
    const userId = tokenManager.getUserId();
    if (!userId) return [];
    const { data } = await apiClient.get<PageResponse<Application>>(`${BASE}/user/${userId}`, {
      params: { page, size },
    });
    return data.content;
  },

  async getApplicationsForJob(jobId: number, page = 0, size = 50): Promise<Application[]> {
    const { data } = await apiClient.get<PageResponse<Application>>(`${BASE}/job/${jobId}`, {
      params: { page, size },
    });
    return data.content;
  },

  async updateStatus(id: number, status: ApplicationStatus, notes?: string): Promise<Application> {
    const { data } = await apiClient.put<Application>(`${BASE}/${id}/status`, { status, notes });
    return data;
  },

  async withdraw(id: number): Promise<Application> {
    const { data } = await apiClient.put<Application>(`${BASE}/${id}/withdraw`);
    return data;
  },
};
