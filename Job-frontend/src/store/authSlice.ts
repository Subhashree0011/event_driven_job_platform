import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit';
import type { User, LoginRequest, RegisterRequest } from '@/types';
import { authService } from '@/services/authService';
import { tokenManager } from '@/security/tokenManager';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
}

const initialState: AuthState = {
  user: null,
  isAuthenticated: tokenManager.isAuthenticated(),
  loading: false,
  error: null,
};

export const loginThunk = createAsyncThunk(
  'auth/login',
  async (credentials: LoginRequest, { rejectWithValue }) => {
    try {
      const response = await authService.login(credentials);
      tokenManager.setTokens(response.accessToken, response.refreshToken);
      const user = await authService.getProfile();
      return user;
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(error.response?.data?.message || error.message || 'Login failed');
    }
  }
);

export const registerThunk = createAsyncThunk(
  'auth/register',
  async (data: RegisterRequest, { rejectWithValue }) => {
    try {
      const response = await authService.register(data);
      tokenManager.setTokens(response.accessToken, response.refreshToken);
      const user = await authService.getProfile();
      return user;
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(error.response?.data?.message || error.message || 'Registration failed');
    }
  }
);

export const logoutThunk = createAsyncThunk('auth/logout', async () => {
  try {
    await authService.logout();
  } finally {
    tokenManager.clearTokens();
  }
});

export const fetchProfileThunk = createAsyncThunk(
  'auth/fetchProfile',
  async (_, { rejectWithValue }) => {
    try {
      return await authService.getProfile();
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(error.response?.data?.message || error.message || 'Failed to fetch profile');
    }
  }
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    clearError(state) {
      state.error = null;
    },
    setUser(state, action: PayloadAction<User>) {
      state.user = action.payload;
      state.isAuthenticated = true;
    },
    forceLogout(state) {
      state.user = null;
      state.isAuthenticated = false;
      tokenManager.clearTokens();
    },
  },
  extraReducers: (builder) => {
    // Login
    builder
      .addCase(loginThunk.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(loginThunk.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload;
        state.isAuthenticated = true;
      })
      .addCase(loginThunk.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });

    // Register
    builder
      .addCase(registerThunk.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(registerThunk.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload;
        state.isAuthenticated = true;
      })
      .addCase(registerThunk.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });

    // Logout
    builder.addCase(logoutThunk.fulfilled, (state) => {
      state.user = null;
      state.isAuthenticated = false;
      state.loading = false;
      state.error = null;
    });

    // Fetch Profile
    builder
      .addCase(fetchProfileThunk.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchProfileThunk.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload;
        state.isAuthenticated = true;
      })
      .addCase(fetchProfileThunk.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });
  },
});

export const { clearError, setUser, forceLogout } = authSlice.actions;
export default authSlice.reducer;
