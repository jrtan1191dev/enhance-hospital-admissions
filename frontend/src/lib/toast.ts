/**
 * Toast notification message data model.
 */
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

/**
 * Global reactive store for toast notifications.
 */
export const toastStore = {
  /**
   * Adds a toast message and auto-dismisses it after 5 seconds.
   *
   * @param item - Notification properties without unique ID.
   */
  add: (item: Omit<ToastMessage, 'id'>) => {
    const id = Math.random().toString(36).substring(2, 9);
    toasts = [...toasts, { ...item, id }];
    emitChange();

    setTimeout(() => {
      toastStore.dismiss(id);
    }, 5000);
  },
  /**
   * Dismisses an active toast by its unique ID.
   *
   * @param id - ID of toast to dismiss.
   */
  dismiss: (id: string) => {
    toasts = toasts.filter((t) => t.id !== id);
    emitChange();
  },
  /**
   * Subscribes a listener to toast store state changes.
   *
   * @param listener - Callback triggered on change.
   * @returns Unsubscribe callback.
   */
  subscribe: (listener: () => void) => {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  },
  /**
   * Retrieves the current snapshot array of active toasts.
   *
   * @returns Array of current toast messages.
   */
  getSnapshot: () => toasts,
};

/**
 * Convenient toast trigger functions for error, success, and informational alerts.
 */
export const toast = {
  /** Displays an error alert toast. */
  error: (title: string, description?: string) => toastStore.add({ type: 'error', title, description }),
  /** Displays a success confirmation toast. */
  success: (title: string, description?: string) => toastStore.add({ type: 'success', title, description }),
  /** Displays an informational notification toast. */
  info: (title: string, description?: string) => toastStore.add({ type: 'info', title, description }),
};
