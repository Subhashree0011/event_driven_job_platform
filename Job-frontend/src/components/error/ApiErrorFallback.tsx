import { AlertCircle, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface ApiErrorFallbackProps {
  error: string;
  onRetry?: () => void;
}

export function ApiErrorFallback({ error, onRetry }: ApiErrorFallbackProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-4 py-12">
      <AlertCircle className="h-12 w-12 text-destructive" />
      <div className="text-center">
        <h3 className="text-lg font-semibold">Something went wrong</h3>
        <p className="mt-1 text-sm text-muted-foreground">{error}</p>
      </div>
      {onRetry && (
        <Button variant="outline" onClick={onRetry} className="gap-2">
          <RefreshCw className="h-4 w-4" />
          Try Again
        </Button>
      )}
    </div>
  );
}
