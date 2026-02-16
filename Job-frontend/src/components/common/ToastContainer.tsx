import { useEffect } from 'react';
import { useAppSelector, useAppDispatch } from '../../store/hooks';
import { removeToast } from '../../store/uiSlice';
import { X, CheckCircle, AlertCircle, AlertTriangle, Info } from 'lucide-react';
import { cn } from '../../lib/utils';
import type { ToastType, Toast } from '../../types';
import type { RootState } from '../../store';

const toastIcons: Record<ToastType, React.ReactNode> = {
  success: <CheckCircle className="h-5 w-5 text-green-500" />,
  error: <AlertCircle className="h-5 w-5 text-red-500" />,
  warning: <AlertTriangle className="h-5 w-5 text-amber-500" />,
  info: <Info className="h-5 w-5 text-blue-500" />,
};

const toastStyles: Record<ToastType, string> = {
  success: 'border-green-200 bg-green-50',
  error: 'border-red-200 bg-red-50',
  warning: 'border-amber-200 bg-amber-50',
  info: 'border-blue-200 bg-blue-50',
};

export function ToastContainer() {
  const dispatch = useAppDispatch();
  const toasts = useAppSelector((state: RootState) => state.ui.toasts);

  return (
    <div className="fixed right-4 top-4 z-100 flex flex-col gap-2">
      {toasts.map((toast: Toast) => (
        <ToastItem
          key={toast.id}
          id={toast.id}
          type={toast.type}
          message={toast.message}
          duration={toast.duration}
          onRemove={() => dispatch(removeToast(toast.id))}
        />
      ))}
    </div>
  );
}

function ToastItem({
  id,
  type,
  message,
  duration = 5000,
  onRemove,
}: {
  id: string;
  type: ToastType;
  message: string;
  duration?: number;
  onRemove: () => void;
}) {
  useEffect(() => {
    const timer = setTimeout(onRemove, duration);
    return () => clearTimeout(timer);
  }, [id, duration, onRemove]);

  return (
    <div
      className={cn(
        'flex items-center gap-3 rounded-lg border p-4 shadow-lg animate-in slide-in-from-right-5 fade-in-0',
        toastStyles[type]
      )}
    >
      {toastIcons[type]}
      <p className="flex-1 text-sm font-medium text-foreground">{message}</p>
      <button onClick={onRemove} className="rounded-sm p-1 opacity-70 hover:opacity-100">
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}
