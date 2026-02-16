import { Suspense, useEffect } from 'react';
import { Navbar } from '@/components/layout/Navbar';
import { Footer } from '@/components/layout/Footer';
import { ToastContainer } from '@/components/common/ToastContainer';
import { Loader } from '@/components/common/Loader';
import { ErrorBoundary } from '@/app/ErrorBoundary';
import { AppRoutes } from '@/app/routes';
import { useAppDispatch } from '@/store/hooks';
import { fetchProfileThunk } from '@/store/authSlice';
import { tokenManager } from '@/security/tokenManager';
import { prefetchCommonRoutes } from '@/performance/prefetch';

export default function App() {
  const dispatch = useAppDispatch();

  /* hydrate auth on mount if a token exists */
  useEffect(() => {
    const token = tokenManager.getAccessToken();
    if (token) {
      dispatch(fetchProfileThunk());
    }
  }, [dispatch]);

  /* prefetch common chunks after first paint */
  useEffect(() => {
    prefetchCommonRoutes();
  }, []);

  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />

      <main className="flex-1">
        <ErrorBoundary>
          <Suspense fallback={<Loader fullPage />}>
            <AppRoutes />
          </Suspense>
        </ErrorBoundary>
      </main>

      <Footer />
      <ToastContainer />
    </div>
  );
}
