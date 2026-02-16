import apiClient from './apiClient';
import type { LoadTestConfig, LoadTestResult } from '@/types';

/**
 * Load test service — fires real HTTP requests to test the backend pipeline.
 * Requests are sent in parallel batches of `concurrency` size.
 */
export const loadTestService = {
  async runLoadTest(
    jobId: number,
    jobTitle: string,
    config: LoadTestConfig,
    onProgress: (completed: number, total: number) => void
  ): Promise<LoadTestResult> {
    const { totalRequests, concurrency, bypassRateLimit } = config;
    const startTime = performance.now();

    const results: Array<{
      status: number;
      latency: number;
      success: boolean;
      error?: string;
      pipeline?: {
        kafka?: { published: boolean };
        database?: { saved: boolean };
        redis?: { hit: boolean };
      };
    }> = [];

    let completed = 0;

    // Send in batches
    for (let i = 0; i < totalRequests; i += concurrency) {
      const batchSize = Math.min(concurrency, totalRequests - i);
      const batch = Array.from({ length: batchSize }, () =>
        sendSingleRequest(jobId, bypassRateLimit)
      );

      const batchResults = await Promise.allSettled(batch);

      for (const result of batchResults) {
        if (result.status === 'fulfilled') {
          results.push(result.value);
        } else {
          results.push({
            status: 0,
            latency: 0,
            success: false,
            error: result.reason?.message || 'Unknown error',
          });
        }
      }

      completed += batchSize;
      onProgress(completed, totalRequests);
    }

    const endTime = performance.now();
    const duration = endTime - startTime;

    return aggregateResults(jobId, jobTitle, config, results, duration);
  },
};

async function sendSingleRequest(
  jobId: number,
  bypassRateLimit: boolean
): Promise<{
  status: number;
  latency: number;
  success: boolean;
  error?: string;
  pipeline?: {
    kafka?: { published: boolean };
    database?: { saved: boolean };
    redis?: { hit: boolean };
  };
}> {
  const start = performance.now();
  try {
    const headers: Record<string, string> = {};
    if (bypassRateLimit) {
      headers['X-Test-Mode'] = 'true';
    }

    const { data, status } = await apiClient.post(
      `/api/v1/applications/load-test`,
      { jobId },
      { headers }
    );

    const latency = performance.now() - start;
    return {
      status,
      latency,
      success: status >= 200 && status < 300,
      pipeline: data?.pipeline,
    };
  } catch (err: unknown) {
    const error = err as { response?: { status?: number; data?: { message?: string } }; message?: string };
    const latency = performance.now() - start;
    return {
      status: error.response?.status || 0,
      latency,
      success: false,
      error: error.response?.data?.message || error.message || 'Request failed',
    };
  }
}

function aggregateResults(
  jobId: number,
  jobTitle: string,
  config: LoadTestConfig,
  results: Array<{
    status: number;
    latency: number;
    success: boolean;
    error?: string;
    pipeline?: {
      kafka?: { published: boolean };
      database?: { saved: boolean };
      redis?: { hit: boolean };
    };
  }>,
  duration: number
): LoadTestResult {
  const latencies = results.map((r) => r.latency).sort((a, b) => a - b);
  const successCount = results.filter((r) => r.success).length;
  const failureCount = results.length - successCount;
  const rateLimitedCount = results.filter((r) => r.status === 429).length;

  // Status code distribution
  const statusCodes: Record<string, number> = {};
  for (const r of results) {
    const key = String(r.status);
    statusCodes[key] = (statusCodes[key] || 0) + 1;
  }

  // Unique errors
  const errorSet = new Set<string>();
  for (const r of results) {
    if (r.error) errorSet.add(r.error);
  }

  // Pipeline aggregation
  const pipeline = {
    kafka: { published: 0, failed: 0 },
    database: { saved: 0, failed: 0 },
    redis: { hits: 0, misses: 0 },
  };
  for (const r of results) {
    if (r.pipeline?.kafka) {
      r.pipeline.kafka.published ? pipeline.kafka.published++ : pipeline.kafka.failed++;
    }
    if (r.pipeline?.database) {
      r.pipeline.database.saved ? pipeline.database.saved++ : pipeline.database.failed++;
    }
    if (r.pipeline?.redis) {
      r.pipeline.redis.hit ? pipeline.redis.hits++ : pipeline.redis.misses++;
    }
  }

  const p = (pct: number) => latencies[Math.floor((latencies.length - 1) * pct)] || 0;

  return {
    id: `test-${Date.now()}`,
    jobId,
    jobTitle,
    config,
    timestamp: new Date().toISOString(),
    duration,
    totalRequests: results.length,
    successCount,
    failureCount,
    avgLatency: latencies.reduce((a, b) => a + b, 0) / latencies.length || 0,
    minLatency: latencies[0] || 0,
    maxLatency: latencies[latencies.length - 1] || 0,
    p95Latency: p(0.95),
    p99Latency: p(0.99),
    throughput: (results.length / duration) * 1000,
    rateLimitedCount,
    statusCodes,
    errors: Array.from(errorSet).slice(0, 10),
    pipeline,
  };
}
