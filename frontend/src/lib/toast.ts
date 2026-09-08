export interface ToastMessage {
  id: string;
  type: 'error' | 'success' | 'info';
  title: string;
  description?: string;
}

let toasts: ToastMessage[] = [];
const listeners = new Set<() => void>();

function emitChange() {
  for (const listener of listeners) {
    listener();
  }
}

export const toastStore = {
  add: (item: Omit<ToastMessage, 'id'>) => {
    const id = Math.random().toString(36).substring(2, 9);
    toasts = [...toasts, { ...item, id }];
    emitChange();

    setTimeout(() => {
      toastStore.dismiss(id);
    }, 5000);
  },
  dismiss: (id: string) => {
    toasts = toasts.filter((t) => t.id !== id);
    emitChange();
  },
  subscribe: (listener: () => void) => {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  },
  getSnapshot: () => toasts,
};

export const toast = {
  error: (title: string, description?: string) => toastStore.add({ type: 'error', title, description }),
  success: (title: string, description?: string) => toastStore.add({ type: 'success', title, description }),
  info: (title: string, description?: string) => toastStore.add({ type: 'info', title, description }),
};
