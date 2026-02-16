/**
 * Prefetch a route's JS chunk by calling the lazy factory again.
 * React.lazy caches, so subsequent calls are no-ops.
 */
const chunkMap: Record<string, () => Promise<unknown>> = {
  '/login': () => import('@/pages/auth/LoginPage'),
  '/register': () => import('@/pages/auth/RegisterPage'),
  '/jobs': () => import('@/pages/jobs/JobListPage'),
  '/dashboard': () => import('@/pages/analytics/DashboardPage'),
  '/profile': () => import('@/pages/profile/ProfilePage'),
};

export function prefetchRoute(path: string) {
  const loader = chunkMap[path];
  if (loader) {
    loader();
  }
}

/**
 * Prefetch common routes after initial load.
 */
export function prefetchCommonRoutes() {
  if (typeof requestIdleCallback === 'function') {
    requestIdleCallback(() => {
      prefetchRoute('/jobs');
      prefetchRoute('/dashboard');
    });
  } else {
    setTimeout(() => {
      prefetchRoute('/jobs');
      prefetchRoute('/dashboard');
    }, 2000);
  }
}
