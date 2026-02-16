import { AxiosError } from 'axios';

interface ApiError {
  message: string;
  status?: number;
  errors?: Record<string, string>;
}

export function extractApiError(error: unknown): ApiError {
  if (error instanceof AxiosError) {
    const data = error.response?.data;
    if (data) {
      return {
        message: data.message || data.error || 'An error occurred',
        status: error.response?.status,
        errors: data.errors,
      };
    }
    if (error.message === 'Network Error') {
      return { message: 'Unable to connect to the server. Please check your connection.' };
    }
    return { message: error.message };
  }
  if (error instanceof Error) {
    return { message: error.message };
  }
  return { message: 'An unexpected error occurred' };
}
