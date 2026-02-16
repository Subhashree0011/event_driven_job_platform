import { useMemo, useState } from 'react';
import { useTestMode } from '@/hooks/useTestMode';
import { useAppDispatch } from '@/store/hooks';
import { clearTestResults } from '@/store/testModeSlice';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import type { LoadTestResult } from '@/types';
import {
  Activity,
  AlertTriangle,
  CheckCircle,
  ChevronDown,
  ChevronUp,
  Clock,
  Server,
  Trash2,
  XCircle,
  Zap,
} from 'lucide-react';

type FilterTab = 'all' | 'passed' | 'failed';

export default function TestResultsPage() {
  const { isTestMode, testResults } = useTestMode();
  const dispatch = useAppDispatch();
  const [filter, setFilter] = useState<FilterTab>('all');
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const getSuccessRate = (r: LoadTestResult) =>
    r.totalRequests > 0 ? (r.successCount / r.totalRequests) * 100 : 0;

  const passed = useMemo(
    () => testResults.filter((r: LoadTestResult) => getSuccessRate(r) >= 95),
    [testResults],
  );
  const failed = useMemo(
    () => testResults.filter((r: LoadTestResult) => getSuccessRate(r) < 95),
    [testResults],
  );
  const filtered =
    filter === 'passed' ? passed : filter === 'failed' ? failed : testResults;

  /* aggregate stats */
  const avgLatency =
    testResults.length > 0
      ? Math.round(
          testResults.reduce((s: number, r: LoadTestResult) => s + r.avgLatency, 0) / testResults.length,
        )
      : 0;
  const avgSuccessRate =
    testResults.length > 0
      ? (
          testResults.reduce((s: number, r: LoadTestResult) => s + getSuccessRate(r), 0) / testResults.length
        ).toFixed(1)
      : '0';

  const toggle = (id: string) =>
    setExpandedId((prev) => (prev === id ? null : id));

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      {/* Warning banner */}
      {!isTestMode && (
        <div className="mb-6 flex items-center gap-3 rounded-lg border border-yellow-300 bg-yellow-50 px-4 py-3 text-sm text-yellow-800">
          <AlertTriangle className="h-5 w-5 shrink-0" />
          <p>
            Testing mode is <strong>OFF</strong>. Enable it from the navbar to
            run new load tests.
          </p>
        </div>
      )}

      <div className="mb-8 flex items-center justify-between">
        <h1 className="text-2xl font-bold">Test Results</h1>
        {testResults.length > 0 && (
          <Button
            variant="outline"
            size="sm"
            className="gap-1 text-destructive hover:text-destructive"
            onClick={() => dispatch(clearTestResults())}
          >
            <Trash2 className="h-4 w-4" />
            Clear All
          </Button>
        )}
      </div>

      {/* Aggregate overview */}
      <div className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        {[
          { icon: Activity, label: 'Total Tests', value: testResults.length },
          { icon: CheckCircle, label: 'Passed', value: passed.length },
          { icon: XCircle, label: 'Failed', value: failed.length },
          { icon: Clock, label: 'Avg Latency', value: `${avgLatency}ms` },
          { icon: Zap, label: 'Avg Success', value: `${avgSuccessRate}%` },
        ].map((s) => (
          <Card key={s.label}>
            <CardContent className="flex items-center gap-3 py-4">
              <s.icon className="h-5 w-5 text-primary" />
              <div>
                <p className="text-xs text-muted-foreground">{s.label}</p>
                <p className="text-lg font-bold">{s.value}</p>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Filter tabs */}
      <div className="mb-6 flex gap-2">
        {(['all', 'passed', 'failed'] as FilterTab[]).map((tab) => (
          <Button
            key={tab}
            variant={filter === tab ? 'default' : 'outline'}
            size="sm"
            onClick={() => setFilter(tab)}
            className="capitalize"
          >
            {tab} (
            {tab === 'all'
              ? testResults.length
              : tab === 'passed'
                ? passed.length
                : failed.length}
            )
          </Button>
        ))}
      </div>

      {/* Results list */}
      {filtered.length === 0 ? (
        <Card>
          <CardContent className="py-16 text-center text-muted-foreground">
            {testResults.length === 0
              ? 'No test results yet. Run a load test from any job detail page.'
              : 'No results match the selected filter.'}
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {filtered.map((result: LoadTestResult) => (
            <ResultCard
              key={result.id}
              result={result}
              expanded={expandedId === result.id}
              onToggle={() => toggle(result.id)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

/* --------------- single result card --------------- */

function ResultCard({
  result,
  expanded,
  onToggle,
}: {
  result: LoadTestResult;
  expanded: boolean;
  onToggle: () => void;
}) {
  const successRate = result.totalRequests > 0 ? (result.successCount / result.totalRequests) * 100 : 0;
  const isPassed = successRate >= 95;

  return (
    <Card className={`transition-shadow ${expanded ? 'shadow-md' : ''}`}>
      {/* Header row */}
      <button
        onClick={onToggle}
        className="flex w-full items-center justify-between px-6 py-4 text-left"
      >
        <div className="flex items-center gap-3">
          {isPassed ? (
            <CheckCircle className="h-5 w-5 text-green-500" />
          ) : (
            <XCircle className="h-5 w-5 text-destructive" />
          )}
          <div>
            <p className="font-medium">Job #{result.jobId}</p>
            <p className="text-xs text-muted-foreground">
              {new Date(result.timestamp).toLocaleString()}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <Badge variant={isPassed ? 'success' : 'destructive'}>
            {successRate.toFixed(1)}% success
          </Badge>
          <span className="text-sm text-muted-foreground">
            {result.avgLatency}ms avg
          </span>
          {expanded ? (
            <ChevronUp className="h-4 w-4 text-muted-foreground" />
          ) : (
            <ChevronDown className="h-4 w-4 text-muted-foreground" />
          )}
        </div>
      </button>

      {/* Expanded detail */}
      {expanded && (
        <CardContent className="border-t pt-4">
          <div className="grid gap-6 md:grid-cols-2">
            {/* Latency stats */}
            <div>
              <h4 className="mb-3 text-sm font-semibold">Latency Breakdown</h4>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Min</span>
                  <span className="font-mono">{result.minLatency}ms</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Average</span>
                  <span className="font-mono">{result.avgLatency}ms</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Max</span>
                  <span className="font-mono">{result.maxLatency}ms</span>
                </div>
                {result.p95Latency != null && (
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">P95</span>
                    <span className="font-mono">{result.p95Latency}ms</span>
                  </div>
                )}
                {result.p99Latency != null && (
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">P99</span>
                    <span className="font-mono">{result.p99Latency}ms</span>
                  </div>
                )}
              </div>
            </div>

            {/* Status codes */}
            <div>
              <h4 className="mb-3 text-sm font-semibold">Status Codes</h4>
              <div className="space-y-2">
                {result.statusCodes &&
                  Object.entries(result.statusCodes).map(([code, count]) => {
                    const ok = code.startsWith('2');
                    return (
                      <div key={code} className="flex items-center justify-between text-sm">
                        <Badge variant={ok ? 'success' : 'destructive'} className="font-mono">
                          {code}
                        </Badge>
                        <span>{count} requests</span>
                      </div>
                    );
                  })}
              </div>
            </div>
          </div>

          {/* Pipeline health */}
          {result.pipeline && (
            <>
              <Separator className="my-4" />
              <div>
                <h4 className="mb-3 flex items-center gap-2 text-sm font-semibold">
                  <Server className="h-4 w-4" />
                  Pipeline Health
                </h4>
                <div className="space-y-1 text-sm">
                  <div className="flex justify-between"><span className="text-muted-foreground">Kafka published</span><span>{result.pipeline.kafka.published}</span></div>
                  <div className="flex justify-between"><span className="text-muted-foreground">Kafka failed</span><span>{result.pipeline.kafka.failed}</span></div>
                  <div className="flex justify-between"><span className="text-muted-foreground">DB saved</span><span>{result.pipeline.database.saved}</span></div>
                  <div className="flex justify-between"><span className="text-muted-foreground">DB failed</span><span>{result.pipeline.database.failed}</span></div>
                  <div className="flex justify-between"><span className="text-muted-foreground">Redis hits</span><span>{result.pipeline.redis.hits}</span></div>
                  <div className="flex justify-between"><span className="text-muted-foreground">Redis misses</span><span>{result.pipeline.redis.misses}</span></div>
                </div>
              </div>
            </>
          )}

          {/* Config summary */}
          <Separator className="my-4" />
          <div className="text-xs text-muted-foreground">
            Config: {result.totalRequests} total requests &middot;{' '}
            {result.config.concurrency} concurrent &middot;{' '}
            {new Date(result.timestamp).toLocaleString()}
          </div>
        </CardContent>
      )}
    </Card>
  );
}
