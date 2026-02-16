import { logger } from '@/utils/logger';

const TOKEN_VERSION = 'jp_v1';
const ACCESS_TOKEN_KEY = `${TOKEN_VERSION}_access_token`;
const REFRESH_TOKEN_KEY = `${TOKEN_VERSION}_refresh_token`;

// In-memory cache to avoid repeated localStorage reads
let cachedAccessToken: string | null = null;
let cachedRefreshToken: string | null = null;

interface JwtPayload {
  sub: string;
  userId: number;
  email: string;
  role: string;
  exp: number;
  iat: number;
}

function decodeJwt(token: string): JwtPayload | null {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    logger.error('Failed to decode JWT');
    return null;
  }
}

export const tokenManager = {
  getAccessToken(): string | null {
    if (cachedAccessToken) return cachedAccessToken;
    cachedAccessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
    return cachedAccessToken;
  },

  getRefreshToken(): string | null {
    if (cachedRefreshToken) return cachedRefreshToken;
    cachedRefreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    return cachedRefreshToken;
  },

  setTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    cachedAccessToken = accessToken;
    cachedRefreshToken = refreshToken;
  },

  clearTokens(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    cachedAccessToken = null;
    cachedRefreshToken = null;
  },

  decodeAccessToken(): JwtPayload | null {
    const token = this.getAccessToken();
    if (!token) return null;
    return decodeJwt(token);
  },

  getUserId(): number | null {
    const payload = this.decodeAccessToken();
    return payload?.userId ?? null;
  },

  isTokenExpired(bufferSeconds = 30): boolean {
    const payload = this.decodeAccessToken();
    if (!payload) return true;
    const nowInSeconds = Math.floor(Date.now() / 1000);
    return payload.exp - bufferSeconds <= nowInSeconds;
  },

  isAuthenticated(): boolean {
    return !!this.getAccessToken() && !this.isTokenExpired();
  },
};
