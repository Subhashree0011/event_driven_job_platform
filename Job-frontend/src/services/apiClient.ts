import axios from 'axios';
import { tokenManager } from '@/security/tokenManager';
import { csrfGuard } from '@/security/csrfGuard';
import { logger } from '@/utils/logger';

const apiClient = axios.create({
  baseURL: '',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Track refresh state to queue concurrent 401 retries
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token!);
    }
  });
  failedQueue = [];
}

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    // Attach access token
    const token = tokenManager.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Attach X-User-Id for dev mode (no API Gateway)
    const userId = tokenManager.getUserId();
    if (userId) {
      config.headers['X-User-Id'] = String(userId);
    }

    // CSRF guard
    if (config.method && csrfGuard.shouldAttach(config.method)) {
      config.headers[csrfGuard.getHeaderName()] = csrfGuard.getToken();
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor — auto-refresh on 401
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/refresh') &&
      !originalRequest.url?.includes('/auth/login')
    ) {
      if (isRefreshing) {
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return apiClient(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshToken = tokenManager.getRefreshToken();
        if (!refreshToken) throw new Error('No refresh token');

        const { data } = await axios.post('/api/v1/auth/refresh', {
          refreshToken,
        });

        tokenManager.setTokens(data.accessToken, data.refreshToken);
        processQueue(null, data.accessToken);

        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        tokenManager.clearTokens();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    logger.error('API Error', {
      url: originalRequest?.url,
      status: error.response?.status,
      data: error.response?.data,
    });

    return Promise.reject(error);
  }
);

export default apiClient;
