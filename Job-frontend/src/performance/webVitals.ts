import { logger } from '@/utils/logger';

type Metric = { name: string; value: number };

function sendMetric(metric: Metric) {
  logger.debug(`[WebVital] ${metric.name}: ${metric.value.toFixed(1)}`);
}

export async function reportWebVitals() {
  if (typeof window === 'undefined') return;
  try {
    const { onCLS, onFID, onLCP, onFCP, onTTFB } = await import('web-vitals');
    onCLS(sendMetric);
    onFID(sendMetric);
    onLCP(sendMetric);
    onFCP(sendMetric);
    onTTFB(sendMetric);
  } catch {
    // web-vitals not installed — silently skip
  }
}
