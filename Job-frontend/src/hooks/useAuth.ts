import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { loginThunk, registerThunk, logoutThunk, fetchProfileThunk, clearError } from '@/store/authSlice';
import type { LoginRequest, RegisterRequest } from '@/types';
import { useCallback } from 'react';

export function useAuth() {
  const dispatch = useAppDispatch();
  const { user, isAuthenticated, loading, error } = useAppSelector((state) => state.auth);

  const login = useCallback(
    (credentials: LoginRequest) => dispatch(loginThunk(credentials)),
    [dispatch]
  );

  const register = useCallback(
    (data: RegisterRequest) => dispatch(registerThunk(data)),
    [dispatch]
  );

  const logout = useCallback(() => dispatch(logoutThunk()), [dispatch]);

  const fetchProfile = useCallback(() => dispatch(fetchProfileThunk()), [dispatch]);

  const resetError = useCallback(() => dispatch(clearError()), [dispatch]);

  return {
    user,
    isAuthenticated,
    loading,
    error,
    login,
    register,
    logout,
    fetchProfile,
    resetError,
  };
}
