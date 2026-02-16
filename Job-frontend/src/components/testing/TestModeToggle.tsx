import { useTestMode } from '@/hooks/useTestMode';
import { Shield, FlaskConical } from 'lucide-react';
import { cn } from '@/lib/utils';

export function TestModeToggle() {
  const { isTestMode, toggle } = useTestMode();

  return (
    <button
      onClick={toggle}
      className={cn(
        'flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-medium transition-all',
        isTestMode
          ? 'bg-amber-500 text-white shadow-lg shadow-amber-500/20'
          : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
      )}
      title={isTestMode ? 'Switch to Production Mode' : 'Switch to Testing Mode'}
    >
      {isTestMode ? (
        <>
          <FlaskConical className="h-3.5 w-3.5" />
          TEST
        </>
      ) : (
        <>
          <Shield className="h-3.5 w-3.5" />
          PROD
        </>
      )}
    </button>
  );
}
