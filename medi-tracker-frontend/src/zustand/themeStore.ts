import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface ThemeState {
  mode: 'light' | 'dark';
  toggleTheme: () => void;
  setTheme: (mode: 'light' | 'dark') => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      mode: 'light',
      toggleTheme: () =>
        set((state) => ({
          mode: state.mode === 'light' ? 'dark' : 'light',
        })),
      setTheme: (mode) => set({ mode }),
    }),
    {
      name: 'meditracker-theme', // localStorage key
    }
  )
);

