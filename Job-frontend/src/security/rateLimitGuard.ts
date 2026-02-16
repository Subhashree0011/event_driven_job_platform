/**
 * Client-side rate limiting guard — sliding window counter.
 * Prevents excessive API calls before they even hit the network.
 */

interface RateLimitEntry {
  timestamps: number[];
  limit: number;
  windowMs: number;
}

const limits: Record<string, RateLimitEntry> = {
  login: { timestamps: [], limit: 5, windowMs: 60_000 },
  register: { timestamps: [], limit: 5, windowMs: 60_000 },
  search: { timestamps: [], limit: 50, windowMs: 60_000 },
  apply: { timestamps: [], limit: 10, windowMs: 60_000 },
  'create-job': { timestamps: [], limit: 10, windowMs: 60_000 },
  'update-profile': { timestamps: [], limit: 10, windowMs: 60_000 },
};

export const rateLimitGuard = {
  /**
   * Check if action is allowed. Returns true if under limit.
   */
  check(action: string): boolean {
    const entry = limits[action];
    if (!entry) return true; // No limit registered

    const now = Date.now();
    // Remove expired timestamps
    entry.timestamps = entry.timestamps.filter((t) => now - t < entry.windowMs);

    if (entry.timestamps.length >= entry.limit) {
      return false;
    }

    entry.timestamps.push(now);
    return true;
  },

  /**
   * Get remaining attempts for an action.
   */
  remaining(action: string): number {
    const entry = limits[action];
    if (!entry) return Infinity;

    const now = Date.now();
    entry.timestamps = entry.timestamps.filter((t) => now - t < entry.windowMs);
    return Math.max(0, entry.limit - entry.timestamps.length);
  },

  /**
   * Reset rate limit for an action.
   */
  reset(action: string): void {
    const entry = limits[action];
    if (entry) entry.timestamps = [];
  },
};
