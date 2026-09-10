import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { toast, toastStore } from './toast';

describe('toastStore and toast helpers', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    // Clear all existing toasts
    for (const t of [...toastStore.getSnapshot()]) {
      toastStore.dismiss(t.id);
    }
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('adds error, success, and info toasts', () => {
    toast.error('Error Title', 'Error Detail');
    toast.success('Success Title');
    toast.info('Info Title', 'Info Detail');

    const snapshots = toastStore.getSnapshot();
    expect(snapshots).toHaveLength(3);
    expect(snapshots[0]).toMatchObject({ type: 'error', title: 'Error Title', description: 'Error Detail' });
    expect(snapshots[1]).toMatchObject({ type: 'success', title: 'Success Title', description: undefined });
    expect(snapshots[2]).toMatchObject({ type: 'info', title: 'Info Title', description: 'Info Detail' });
  });

  it('notifies subscribers when toasts are added and dismissed', () => {
    const listener = vi.fn();
    const unsubscribe = toastStore.subscribe(listener);

    toast.info('Hello');
    expect(listener).toHaveBeenCalledTimes(1);

    const id = toastStore.getSnapshot()[0].id;
    toastStore.dismiss(id);
    expect(listener).toHaveBeenCalledTimes(2);
    expect(toastStore.getSnapshot()).toHaveLength(0);

    unsubscribe();
    toast.success('Ignored by unsubscribed listener');
    expect(listener).toHaveBeenCalledTimes(2);
  });

  it('auto-dismisses toasts after 5 seconds', () => {
    toast.error('Temporary Error');
    expect(toastStore.getSnapshot()).toHaveLength(1);

    vi.advanceTimersByTime(4999);
    expect(toastStore.getSnapshot()).toHaveLength(1);

    vi.advanceTimersByTime(1);
    expect(toastStore.getSnapshot()).toHaveLength(0);
  });
});
