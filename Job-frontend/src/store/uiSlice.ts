import { createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { Toast, ToastType } from '@/types';

interface UiState {
  sidebarOpen: boolean;
  modalOpen: boolean;
  modalContent: string | null;
  toasts: Toast[];
}

const initialState: UiState = {
  sidebarOpen: false,
  modalOpen: false,
  modalContent: null,
  toasts: [],
};

let toastCounter = 0;

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    toggleSidebar(state) {
      state.sidebarOpen = !state.sidebarOpen;
    },
    setSidebarOpen(state, action: PayloadAction<boolean>) {
      state.sidebarOpen = action.payload;
    },
    openModal(state, action: PayloadAction<string>) {
      state.modalOpen = true;
      state.modalContent = action.payload;
    },
    closeModal(state) {
      state.modalOpen = false;
      state.modalContent = null;
    },
    addToast(state, action: PayloadAction<{ type: ToastType; message: string; duration?: number }>) {
      const toast: Toast = {
        id: `toast-${++toastCounter}`,
        type: action.payload.type,
        message: action.payload.message,
        duration: action.payload.duration ?? 5000,
      };
      state.toasts.push(toast);
    },
    removeToast(state, action: PayloadAction<string>) {
      state.toasts = state.toasts.filter((t) => t.id !== action.payload);
    },
    clearToasts(state) {
      state.toasts = [];
    },
  },
});

export const {
  toggleSidebar,
  setSidebarOpen,
  openModal,
  closeModal,
  addToast,
  removeToast,
  clearToasts,
} = uiSlice.actions;
export default uiSlice.reducer;
