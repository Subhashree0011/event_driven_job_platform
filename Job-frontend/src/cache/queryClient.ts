/**
 * Mini in-memory cache with TTL support.
 * Used for caching API responses client-side.
 */

interface CacheEntry<T> {
  data: T;
  timestamp: number;
  staleTime: number;
  cacheTime: number;
}

const cache = new Map<string, CacheEntry<unknown>>();

export const cachePolicy = {
  jobs: { staleTime: 30_000, cacheTime: 300_000 },         // 30s stale, 5min cache
  jobDetail: { staleTime: 60_000, cacheTime: 600_000 },    // 1min stale, 10min cache
  companies: { staleTime: 120_000, cacheTime: 600_000 },   // 2min stale, 10min cache
  profile: { staleTime: 60_000, cacheTime: 300_000 },      // 1min stale, 5min cache
  applications: { staleTime: 15_000, cacheTime: 120_000 }, // 15s stale, 2min cache
} as const;

export type CacheResource = keyof typeof cachePolicy;

export const queryClient = {
  get<T>(key: string): T | null {
    const entry = cache.get(key) as CacheEntry<T> | undefined;
    if (!entry) return null;

    const now = Date.now();
    // Remove if past cache time
    if (now - entry.timestamp > entry.cacheTime) {
      cache.delete(key);
      return null;
    }

    return entry.data;
  },

  isStale(key: string): boolean {
    const entry = cache.get(key);
    if (!entry) return true;
    return Date.now() - entry.timestamp > entry.staleTime;
  },

  set<T>(key: string, data: T, resource: CacheResource): void {
    const policy = cachePolicy[resource];
    cache.set(key, {
      data,
      timestamp: Date.now(),
      staleTime: policy.staleTime,
      cacheTime: policy.cacheTime,
    });
  },

  invalidate(keyPrefix: string): void {
    for (const key of cache.keys()) {
      if (key.startsWith(keyPrefix)) {
        cache.delete(key);
      }
    }
  },

  clear(): void {
    cache.clear();
  },
};
