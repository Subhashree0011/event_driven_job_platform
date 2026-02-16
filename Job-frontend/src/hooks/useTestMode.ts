import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { toggleMode } from '@/store/testModeSlice';
import { useCallback } from 'react';

export function useTestMode() {
  const dispatch = useAppDispatch();
  const { mode, testResults, activeTestJobId } = useAppSelector((state) => state.testMode);

  const isTestMode = mode === 'testing';

  const toggle = useCallback(() => dispatch(toggleMode()), [dispatch]);

  return {
    mode,
    isTestMode,
    testResults,
    activeTestJobId,
    toggle,
  };
}
