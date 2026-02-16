import { createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { LoadTestResult } from '@/types';

type AppMode = 'production' | 'testing';

interface TestModeState {
  mode: AppMode;
  testResults: LoadTestResult[];
  activeTestJobId: number | null;
}

const initialState: TestModeState = {
  mode: 'production',
  testResults: [],
  activeTestJobId: null,
};

const testModeSlice = createSlice({
  name: 'testMode',
  initialState,
  reducers: {
    toggleMode(state) {
      state.mode = state.mode === 'production' ? 'testing' : 'production';
    },
    setMode(state, action: PayloadAction<AppMode>) {
      state.mode = action.payload;
    },
    addTestResult(state, action: PayloadAction<LoadTestResult>) {
      state.testResults.unshift(action.payload);
    },
    clearTestResults(state) {
      state.testResults = [];
    },
    setActiveTestJobId(state, action: PayloadAction<number | null>) {
      state.activeTestJobId = action.payload;
    },
  },
});

export const { toggleMode, setMode, addTestResult, clearTestResults, setActiveTestJobId } =
  testModeSlice.actions;
export default testModeSlice.reducer;
