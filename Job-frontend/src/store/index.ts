import { combineReducers, configureStore } from '@reduxjs/toolkit';
import authReducer from './authSlice';
import uiReducer from './uiSlice';
import testModeReducer from './testModeSlice';

const rootReducer = combineReducers({
  auth: authReducer,
  ui: uiReducer,
  testMode: testModeReducer,
});

export type RootState = ReturnType<typeof rootReducer>;

export const store = configureStore({
  reducer: rootReducer,
});

export type AppDispatch = typeof store.dispatch;
