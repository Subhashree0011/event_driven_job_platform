import { FlaskConical } from 'lucide-react';

export function TestModeBanner() {
  return (
    <div className="flex items-center justify-center gap-2 bg-linear-to-r from-amber-500 to-orange-500 px-4 py-2 text-sm font-medium text-white">
      <FlaskConical className="h-4 w-4" />
      TESTING MODE ACTIVE — Rate limits are bypassed
    </div>
  );
}
