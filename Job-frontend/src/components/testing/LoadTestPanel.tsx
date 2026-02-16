import { useState } from 'react';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import type { RootState } from '../../store';
import { addTestResult, setActiveTestJobId } from '../../store/testModeSlice';
import { loadTestService } from '../../services/loadTestService';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Badge } from '../ui/badge';
import { FlaskConical, Play, Loader2 } from 'lucide-react';
import { cn } from '../../lib/utils';
import { addToast } from '../../store/uiSlice';

interface LoadTestPanelProps {
  jobId: number;
  jobTitle: string;
}

const PRESETS = [
  { label: '100 req', requests: 100, concurrency: 10 },
  { label: '500 req', requests: 500, concurrency: 25 },
  { label: '1K req', requests: 1000, concurrency: 50 },
  { label: '5K req', requests: 5000, concurrency: 100 },
  { label: '10K req', requests: 10000, concurrency: 200 },
];

export function LoadTestPanel({ jobId, jobTitle }: LoadTestPanelProps) {
  const dispatch = useAppDispatch();
  const activeTestJobId = useAppSelector((state: RootState) => state.testMode.activeTestJobId);

  const [totalRequests, setTotalRequests] = useState(100);
  const [concurrency, setConcurrency] = useState(10);
  const [bypassRateLimit, setBypassRateLimit] = useState(true);
  const [running, setRunning] = useState(false);
  const [progress, setProgress] = useState({ completed: 0, total: 0 });

  const isAnotherTestRunning = activeTestJobId !== null && activeTestJobId !== jobId;

  const handleRun = async () => {
    if (isAnotherTestRunning) return;

    setRunning(true);
    dispatch(setActiveTestJobId(jobId));
    setProgress({ completed: 0, total: totalRequests });

    try {
      const result = await loadTestService.runLoadTest(
        jobId,
        jobTitle,
        { totalRequests, concurrency, bypassRateLimit },
        (completed, total) => setProgress({ completed, total })
      );

      dispatch(addTestResult(result));
      dispatch(addToast({ type: 'success', message: `Load test completed: ${result.successCount}/${result.totalRequests} successful` }));
    } catch {
      dispatch(addToast({ type: 'error', message: 'Load test failed' }));
    } finally {
      setRunning(false);
      dispatch(setActiveTestJobId(null));
    }
  };

  const progressPct = progress.total > 0 ? Math.round((progress.completed / progress.total) * 100) : 0;

  return (
    <div className="mt-4 rounded-lg border border-amber-200 bg-amber-50/50 p-4">
      <div className="mb-3 flex items-center gap-2">
        <FlaskConical className="h-4 w-4 text-amber-600" />
        <h4 className="text-sm font-semibold text-amber-800">Load Test Panel</h4>
      </div>

      {/* Presets */}
      <div className="mb-3 flex flex-wrap gap-1.5">
        {PRESETS.map((preset) => (
          <button
            key={preset.label}
            onClick={() => {
              setTotalRequests(preset.requests);
              setConcurrency(preset.concurrency);
            }}
            className={cn(
              'rounded-md px-2.5 py-1 text-xs font-medium transition-colors',
              totalRequests === preset.requests
                ? 'bg-amber-500 text-white'
                : 'bg-white text-amber-700 border border-amber-200 hover:bg-amber-100'
            )}
            disabled={running}
          >
            {preset.label}
          </button>
        ))}
      </div>

      {/* Configuration */}
      <div className="mb-3 grid grid-cols-2 gap-3">
        <div>
          <label className="mb-1 block text-xs font-medium text-amber-700">Total Requests</label>
          <Input
            type="number"
            min={1}
            max={50000}
            value={totalRequests}
            onChange={(e) => setTotalRequests(Number(e.target.value))}
            disabled={running}
            className="h-8 text-sm"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-amber-700">Concurrency</label>
          <Input
            type="number"
            min={1}
            max={1000}
            value={concurrency}
            onChange={(e) => setConcurrency(Number(e.target.value))}
            disabled={running}
            className="h-8 text-sm"
          />
        </div>
      </div>

      {/* Bypass Rate Limit */}
      <label className="mb-3 flex items-center gap-2 text-xs">
        <input
          type="checkbox"
          checked={bypassRateLimit}
          onChange={(e) => setBypassRateLimit(e.target.checked)}
          disabled={running}
          className="rounded"
        />
        <span className="text-amber-700">Bypass Rate Limit (sends X-Test-Mode header)</span>
      </label>

      {/* Run Button */}
      <Button
        onClick={handleRun}
        disabled={running || isAnotherTestRunning}
        className="w-full gap-2 bg-amber-500 hover:bg-amber-600"
        size="sm"
      >
        {running ? (
          <>
            <Loader2 className="h-4 w-4 animate-spin" />
            Running... {progressPct}%
          </>
        ) : (
          <>
            <Play className="h-4 w-4" />
            Run Load Test
          </>
        )}
      </Button>

      {/* Progress Bar */}
      {running && (
        <div className="mt-2">
          <div className="h-2 w-full overflow-hidden rounded-full bg-amber-200">
            <div
              className="h-full rounded-full bg-amber-500 transition-all duration-300"
              style={{ width: `${progressPct}%` }}
            />
          </div>
          <p className="mt-1 text-center text-xs text-amber-600">
            {progress.completed} / {progress.total}
          </p>
        </div>
      )}

      {isAnotherTestRunning && (
        <Badge variant="warning" className="mt-2">
          Another test is running on a different job
        </Badge>
      )}
    </div>
  );
}
