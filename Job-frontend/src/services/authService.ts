import apiClient from './apiClient';
import type { User, LoginRequest, RegisterRequest, AuthTokens, UpdateProfileRequest } from '@/types';

const AUTH_BASE = '/api/v1/auth';
const USER_BASE = '/api/v1/users';

export const authService = {
  async login(credentials: LoginRequest): Promise<AuthTokens> {
    const { data } = await apiClient.post<AuthTokens>(`${AUTH_BASE}/login`, credentials);
    return data;
  },

  async register(data: RegisterRequest): Promise<AuthTokens> {
    const { data: result } = await apiClient.post<AuthTokens>(`${AUTH_BASE}/register`, data);
    return result;
  },

  async logout(): Promise<void> {
    const refreshToken = (await import('@/security/tokenManager')).tokenManager.getRefreshToken();
    await apiClient.post(`${AUTH_BASE}/logout`, refreshToken ? { refreshToken } : undefined);
  },

  async getProfile(): Promise<User> {
    const { data } = await apiClient.get<User>(`${USER_BASE}/me`);
    return data;
  },

  async updateProfile(profileData: UpdateProfileRequest): Promise<User> {
    const { data } = await apiClient.put<User>(`${USER_BASE}/me`, profileData);
    return data;
  },

  async getPublicProfile(userId: number): Promise<User> {
    const { data } = await apiClient.get<User>(`${USER_BASE}/${userId}`);
    return data;
  },
};
