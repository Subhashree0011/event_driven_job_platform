import { useDispatch, useSelector } from 'react-redux';
import type { RootState, AppDispatch } from './index';

export function useAppDispatch() {
  return useDispatch<AppDispatch>();
}

export function useAppSelector<T>(selector: (state: RootState) => T): T {
  return useSelector(selector);
}
