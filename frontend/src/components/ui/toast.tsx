import { useSyncExternalStore } from 'react';
import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react';
import { toastStore } from '@/lib/toast';

export function ToastContainer() {
  const items = useSyncExternalStore(toastStore.subscribe, toastStore.getSnapshot);

  if (items.length === 0) return null;

  return (
    <div
      aria-live="polite"
      className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 max-w-sm sm:max-w-md w-full pointer-events-none px-4 sm:px-0"
    >
      {items.map((t) => {
        const isError = t.type === 'error';
        const isSuccess = t.type === 'success';

        return (
          <div
            key={t.id}
            role="alert"
            className={`pointer-events-auto flex items-start gap-3 p-3.5 rounded-xl shadow-lg border backdrop-blur-xs transition-all ${
              isError
                ? 'bg-white/95 border-rose-200 text-rose-950'
                : isSuccess
                  ? 'bg-white/95 border-emerald-200 text-emerald-950'
                  : 'bg-white/95 border-slate-200 text-slate-900'
            }`}
          >
            <div className="shrink-0 mt-0.5">
              {isError && <AlertCircle className="w-5 h-5 text-rose-600" />}
              {isSuccess && <CheckCircle2 className="w-5 h-5 text-emerald-600" />}
              {!isError && !isSuccess && <Info className="w-5 h-5 text-blue-600" />}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-semibold tracking-tight">{t.title}</p>
              {t.description && (
                <p className="text-xs text-slate-600 mt-0.5 leading-relaxed break-words">
                  {t.description}
                </p>
              )}
            </div>
            <button
              onClick={() => toastStore.dismiss(t.id)}
              className="shrink-0 text-slate-400 hover:text-slate-700 p-0.5 rounded-md hover:bg-slate-100 transition-colors cursor-pointer"
              aria-label="Close notification"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        );
      })}
    </div>
  );
}
