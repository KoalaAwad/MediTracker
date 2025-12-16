import { create } from 'zustand';
import { authApi, LoginRequest, User } from '../api/authApi';

type AuthState = {
  // State
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;

  init: () => Promise<void>;
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: null,
  isAuthenticated: false,
  isLoading: true,
  error: null,

  init: async () => {
    const storedToken = localStorage.getItem('token');
    if (!storedToken) {
      set({ isLoading: false, user: null, isAuthenticated: false, token: null });
      return;
    }
    try {
      const response = await authApi.getCurrentUser(storedToken);
      set({ user: response.data, token: storedToken, isAuthenticated: true, isLoading: false, error: null });
    } catch (error) {
      console.error('Auth check failed:', error);
      localStorage.removeItem('token');
      set({ user: null, token: null, isAuthenticated: false, isLoading: false, error: 'Session expired' });
    }
  },

  // Login and fetch current user
  login: async (credentials: LoginRequest) => {
    set({ isLoading: true, error: null });
    const response = await authApi.login(credentials);
    const token = response.data.token;

    localStorage.setItem('token', token);

    try {
      const me = await authApi.getCurrentUser(token);
      set({ user: me.data, token, isAuthenticated: true, isLoading: false, error: null });
    } catch (error: any) {
      console.error('Failed to fetch current user after login:', error);
      set({ isLoading: false, error: error?.response?.data?.error || 'Login failed', user: null, token: null, isAuthenticated: false });
      localStorage.removeItem('token');
      throw new Error(error?.response?.data?.error || 'Login failed');
    }
  },

  // Logout
  logout: () => {
    localStorage.removeItem('token');
    set({ user: null, token: null, isAuthenticated: false, error: null });
  },
}));
