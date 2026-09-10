import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  authQueries,
  edQueries,
  specialistQueries,
  bmuQueries,
  patientQueries,
  useSubmitEdAssessment,
  useClaimBroadcast,
  useSubmitConsult,
  useAmendConsult,
  useRequestReconciliation,
  useAssignAdmittingCluster,
  useChainConsult,
  useAllocateBed,
  useDeallocateBed,
  useApproveBatchHoldingWard,
  useApproveCohortSwap,
  useRecallDiversion,
  useExtendDiversionSla,
  useLogTelephoneFollowUp,
  useAttachDelayTag,
  useReferSisterHospital,
  useUpdateBmuConfig,
  useCheckinPatient,
  useVacatePatient,
  useCleanBed,
  useSimulatePeriodicUpdate,
  useRecordPatientAction,
} from './queries';
import { api } from './api';

vi.mock('./api', () => ({
  api: {
    getAuthStatus: vi.fn().mockResolvedValue({ authenticated: true, username: 'test_user' }),
    getEdWaitingPatients: vi.fn().mockResolvedValue([]),
    getEdSubmittedAdmissions: vi.fn().mockResolvedValue([]),
    getSpecialistBroadcasts: vi.fn().mockResolvedValue([]),
    getPrioritizedQueue: vi.fn().mockResolvedValue([]),
    getInventory: vi.fn().mockResolvedValue([]),
    getRecommendations: vi.fn().mockResolvedValue([]),
    getConfig: vi.fn().mockResolvedValue({}),
    getBatchSuggestions: vi.fn().mockResolvedValue([]),
    getCohortSwapSuggestions: vi.fn().mockResolvedValue([]),
    trackPatient: vi.fn().mockResolvedValue({}),
    getAvailablePatients: vi.fn().mockResolvedValue([]),

    submitEdAssessment: vi.fn().mockResolvedValue({ id: 'adm-1' }),
    claimBroadcast: vi.fn().mockResolvedValue({ id: 'bc-1' }),
    submitConsult: vi.fn().mockResolvedValue({ id: 'bc-1' }),
    amendConsult: vi.fn().mockResolvedValue({ id: 'bc-1' }),
    requestReconciliation: vi.fn().mockResolvedValue({ id: 'req-1' }),
    assignAdmittingCluster: vi.fn().mockResolvedValue({ id: 'req-1' }),
    chainConsult: vi.fn().mockResolvedValue({ id: 'bc-1' }),
    allocateBed: vi.fn().mockResolvedValue({ id: 'req-1' }),
    deallocateBed: vi.fn().mockResolvedValue({ id: 'req-1' }),
    approveBatchHoldingWard: vi.fn().mockResolvedValue([]),
    approveCohortSwap: vi.fn().mockResolvedValue({ id: 'req-1' }),
    recallDiversion: vi.fn().mockResolvedValue({ id: 'req-1' }),
    extendDiversionSla: vi.fn().mockResolvedValue({ id: 'req-1' }),
    logTelephoneFollowUp: vi.fn().mockResolvedValue({ id: 'req-1' }),
    attachDelayTag: vi.fn().mockResolvedValue({ id: 'req-1' }),
    referToSisterHospital: vi.fn().mockResolvedValue({ id: 'ref-1' }),
    updateConfig: vi.fn().mockResolvedValue({}),
    checkinPatient: vi.fn().mockResolvedValue({ id: 'bed-1' }),
    vacatePatient: vi.fn().mockResolvedValue({ id: 'bed-1' }),
    cleanBed: vi.fn().mockResolvedValue({ id: 'bed-1' }),
    simulatePeriodicUpdate: vi.fn().mockResolvedValue({ dispatchedCount: 2, message: 'Simulated' }),
    recordPatientAction: vi.fn().mockResolvedValue({ status: 'ACK' }),
  },
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return {
    queryClient,
    wrapper: ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    ),
  };
}

describe('services/queries', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Query Options Factories', () => {
    it('authQueries returns correct query keys and fetcher', async () => {
      const q = authQueries.me();
      expect(q.queryKey).toEqual(['auth', 'me']);
      const res = await q.queryFn();
      expect(res).toEqual({ authenticated: true, username: 'test_user' });
    });

    it('edQueries returns correct query keys and fetchers', async () => {
      const patients = edQueries.patients();
      expect(patients.queryKey).toEqual(['ed', 'patients']);
      expect(patients.refetchInterval).toBe(3000);
      await expect(patients.queryFn()).resolves.toEqual([]);

      const admissions = edQueries.admissions();
      expect(admissions.queryKey).toEqual(['ed', 'admissions']);
      await expect(admissions.queryFn()).resolves.toEqual([]);
    });

    it('specialistQueries returns correct query keys with and without cluster', async () => {
      const allBroadcasts = specialistQueries.broadcasts();
      expect(allBroadcasts.queryKey).toEqual(['specialist', 'broadcasts', 'ALL']);
      await expect(allBroadcasts.queryFn()).resolves.toEqual([]);

      const cardioBroadcasts = specialistQueries.broadcasts('CARDIOLOGY');
      expect(cardioBroadcasts.queryKey).toEqual(['specialist', 'broadcasts', 'CARDIOLOGY']);
      await expect(cardioBroadcasts.queryFn()).resolves.toEqual([]);
      expect(api.getSpecialistBroadcasts).toHaveBeenCalledWith('CARDIOLOGY');
    });

    it('bmuQueries returns correct keys and handles recommendations queryFn', async () => {
      expect(bmuQueries.queue().queryKey).toEqual(['bmu', 'queue']);
      await expect(bmuQueries.queue().queryFn()).resolves.toEqual([]);

      expect(bmuQueries.inventory().queryKey).toEqual(['bmu', 'inventory']);
      await expect(bmuQueries.inventory().queryFn()).resolves.toEqual([]);

      expect(bmuQueries.config().queryKey).toEqual(['bmu', 'config']);
      await expect(bmuQueries.config().queryFn()).resolves.toEqual({});

      expect(bmuQueries.batchSuggestions().queryKey).toEqual(['bmu', 'batchSuggestions']);
      await expect(bmuQueries.batchSuggestions().queryFn()).resolves.toEqual([]);

      expect(bmuQueries.cohortSwapSuggestions().queryKey).toEqual(['bmu', 'cohortSwapSuggestions']);
      await expect(bmuQueries.cohortSwapSuggestions().queryFn()).resolves.toEqual([]);

      const recWithId = bmuQueries.recommendations('req-123');
      expect(recWithId.queryKey).toEqual(['bmu', 'recommendations', 'req-123']);
      expect(recWithId.enabled).toBe(true);
      await expect(recWithId.queryFn()).resolves.toEqual([]);
      expect(api.getRecommendations).toHaveBeenCalledWith('req-123');

      const recWithoutId = bmuQueries.recommendations(undefined);
      expect(recWithoutId.queryKey).toEqual(['bmu', 'recommendations', 'none']);
      expect(recWithoutId.enabled).toBe(false);
      await expect(recWithoutId.queryFn()).resolves.toEqual([]);
    });

    it('patientQueries returns correct keys and handles track queryFn', async () => {
      const trackQ = patientQueries.track('token-xyz');
      expect(trackQ.queryKey).toEqual(['patient', 'track', 'token-xyz']);
      expect(trackQ.enabled).toBe(true);
      await expect(trackQ.queryFn()).resolves.toEqual({});
      expect(api.trackPatient).toHaveBeenCalledWith('token-xyz');

      const availQ = patientQueries.availablePatients();
      expect(availQ.queryKey).toEqual(['patient', 'available']);
      await expect(availQ.queryFn()).resolves.toEqual([]);
    });
  });

  describe('Mutation Hooks', () => {
    it('useSubmitEdAssessment calls api and handles onSuccess', async () => {
      const { wrapper, queryClient } = createWrapper();
      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');
      const onSuccess = vi.fn();

      const { result } = renderHook(() => useSubmitEdAssessment(onSuccess), { wrapper });
      await act(async () => {
        await result.current.mutateAsync({ patientId: 'p1' } as any);
      });

      expect(api.submitEdAssessment).toHaveBeenCalledWith({ patientId: 'p1' });
      expect(invalidateSpy).toHaveBeenCalled();
      expect(onSuccess).toHaveBeenCalled();
    });

    it('useClaimBroadcast handles success and error callbacks', async () => {
      const { wrapper, queryClient } = createWrapper();
      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');
      const onSuccess = vi.fn();
      const onError = vi.fn();

      const { result } = renderHook(() => useClaimBroadcast(onSuccess, onError), { wrapper });
      await act(async () => {
        await result.current.mutateAsync('bc-1');
      });
      expect(api.claimBroadcast).toHaveBeenCalledWith('bc-1');
      expect(invalidateSpy).toHaveBeenCalled();
      expect(onSuccess).toHaveBeenCalled();

      // Test error branch
      vi.mocked(api.claimBroadcast).mockRejectedValueOnce(new Error('Claim failed'));
      await act(async () => {
        try {
          await result.current.mutateAsync('bc-fail');
        } catch {
          // caught by hook
        }
      });
      expect(onError).toHaveBeenCalled();
    });

    it('useSubmitConsult calls api and invalidates queries', async () => {
      const { wrapper } = createWrapper();
      const onSuccess = vi.fn();
      const { result } = renderHook(() => useSubmitConsult(onSuccess), { wrapper });
      await act(async () => {
        await result.current.mutateAsync({ id: 'bc-1', data: {} as any });
      });
      expect(api.submitConsult).toHaveBeenCalled();
      expect(onSuccess).toHaveBeenCalled();
    });

    it('useAmendConsult handles success and error callbacks', async () => {
      const { wrapper } = createWrapper();
      const onSuccess = vi.fn();
      const onError = vi.fn();
      const { result } = renderHook(() => useAmendConsult(onSuccess, onError), { wrapper });
      await act(async () => {
        await result.current.mutateAsync({ id: 'bc-1', data: {} as any });
      });
      expect(api.amendConsult).toHaveBeenCalled();
      expect(onSuccess).toHaveBeenCalled();

      vi.mocked(api.amendConsult).mockRejectedValueOnce(new Error('Amend failed'));
      await act(async () => {
        try {
          await result.current.mutateAsync({ id: 'bc-1', data: {} as any });
        } catch {}
      });
      expect(onError).toHaveBeenCalled();
    });

    it('useRequestReconciliation handles success and error', async () => {
      const { wrapper } = createWrapper();
      const onSuccess = vi.fn();
      const onError = vi.fn();
      const { result } = renderHook(() => useRequestReconciliation(onSuccess, onError), { wrapper });
      await act(async () => {
        await result.current.mutateAsync('req-1');
      });
      expect(api.requestReconciliation).toHaveBeenCalledWith('req-1');
      expect(onSuccess).toHaveBeenCalled();

      vi.mocked(api.requestReconciliation).mockRejectedValueOnce(new Error('Reconcile failed'));
      await act(async () => {
        try {
          await result.current.mutateAsync('req-1');
        } catch {}
      });
      expect(onError).toHaveBeenCalled();
    });

    it('useAssignAdmittingCluster handles success and error', async () => {
      const { wrapper } = createWrapper();
      const onSuccess = vi.fn();
      const onError = vi.fn();
      const { result } = renderHook(() => useAssignAdmittingCluster(onSuccess, onError), { wrapper });
      await act(async () => {
        await result.current.mutateAsync({ requestId: 'r1', cluster: 'CARDIOLOGY' });
      });
      expect(api.assignAdmittingCluster).toHaveBeenCalledWith('r1', 'CARDIOLOGY');
      expect(onSuccess).toHaveBeenCalled();

      vi.mocked(api.assignAdmittingCluster).mockRejectedValueOnce(new Error('Fail'));
      await act(async () => {
        try {
          await result.current.mutateAsync({ requestId: 'r1', cluster: 'CARDIOLOGY' });
        } catch {}
      });
      expect(onError).toHaveBeenCalled();
    });

    it('useChainConsult handles success and error', async () => {
      const { wrapper } = createWrapper();
      const onSuccess = vi.fn();
      const onError = vi.fn();
      const { result } = renderHook(() => useChainConsult(onSuccess, onError), { wrapper });
      await act(async () => {
        await result.current.mutateAsync({ id: 'b1', data: {} as any });
      });
      expect(api.chainConsult).toHaveBeenCalled();
      expect(onSuccess).toHaveBeenCalled();

      vi.mocked(api.chainConsult).mockRejectedValueOnce(new Error('Fail'));
      await act(async () => {
        try {
          await result.current.mutateAsync({ id: 'b1', data: {} as any });
        } catch {}
      });
      expect(onError).toHaveBeenCalled();
    });

    it('useAllocateBed and useDeallocateBed call respective apis', async () => {
      const { wrapper } = createWrapper();
      const allocSuccess = vi.fn();
      const { result: allocResult } = renderHook(() => useAllocateBed(allocSuccess), { wrapper });
      await act(async () => {
        await allocResult.current.mutateAsync({} as any);
      });
      expect(api.allocateBed).toHaveBeenCalled();
      expect(allocSuccess).toHaveBeenCalled();

      const deallocSuccess = vi.fn();
      const { result: deallocResult } = renderHook(() => useDeallocateBed(deallocSuccess), { wrapper });
      await act(async () => {
        await deallocResult.current.mutateAsync('req-1');
      });
      expect(api.deallocateBed).toHaveBeenCalledWith('req-1');
      expect(deallocSuccess).toHaveBeenCalled();
    });

    it('useApproveBatchHoldingWard and useApproveCohortSwap handle operations', async () => {
      const { wrapper } = createWrapper();
      const batchSuccess = vi.fn();
      const batchError = vi.fn();
      const { result: batchResult } = renderHook(() => useApproveBatchHoldingWard(batchSuccess, batchError), { wrapper });
      await act(async () => {
        await batchResult.current.mutateAsync({} as any);
      });
      expect(api.approveBatchHoldingWard).toHaveBeenCalled();
      expect(batchSuccess).toHaveBeenCalled();

      vi.mocked(api.approveBatchHoldingWard).mockRejectedValueOnce(new Error('Batch fail'));
      await act(async () => {
        try {
          await batchResult.current.mutateAsync({} as any);
        } catch {}
      });
      expect(batchError).toHaveBeenCalled();

      const swapSuccess = vi.fn();
      const swapError = vi.fn();
      const { result: swapResult } = renderHook(() => useApproveCohortSwap(swapSuccess, swapError), { wrapper });
      await act(async () => {
        await swapResult.current.mutateAsync({} as any);
      });
      expect(api.approveCohortSwap).toHaveBeenCalled();
      expect(swapSuccess).toHaveBeenCalled();

      vi.mocked(api.approveCohortSwap).mockRejectedValueOnce(new Error('Swap fail'));
      await act(async () => {
        try {
          await swapResult.current.mutateAsync({} as any);
        } catch {}
      });
      expect(swapError).toHaveBeenCalled();
    });

    it('useRecallDiversion and useExtendDiversionSla execute properly', async () => {
      const { wrapper } = createWrapper();
      const recallSuccess = vi.fn();
      const recallError = vi.fn();
      const { result: recallResult } = renderHook(() => useRecallDiversion(recallSuccess, recallError), { wrapper });
      await act(async () => {
        await recallResult.current.mutateAsync('r1');
      });
      expect(api.recallDiversion).toHaveBeenCalledWith('r1');
      expect(recallSuccess).toHaveBeenCalled();

      vi.mocked(api.recallDiversion).mockRejectedValueOnce(new Error('Recall fail'));
      await act(async () => {
        try {
          await recallResult.current.mutateAsync('r1');
        } catch {}
      });
      expect(recallError).toHaveBeenCalled();

      const extSuccess = vi.fn();
      const extError = vi.fn();
      const { result: extResult } = renderHook(() => useExtendDiversionSla(extSuccess, extError), { wrapper });
      await act(async () => {
        await extResult.current.mutateAsync('r1');
      });
      expect(api.extendDiversionSla).toHaveBeenCalledWith('r1');
      expect(extSuccess).toHaveBeenCalled();

      vi.mocked(api.extendDiversionSla).mockRejectedValueOnce(new Error('Ext fail'));
      await act(async () => {
        try {
          await extResult.current.mutateAsync('r1');
        } catch {}
      });
      expect(extError).toHaveBeenCalled();
    });

    it('useLogTelephoneFollowUp and useAttachDelayTag execute properly', async () => {
      const { wrapper } = createWrapper();
      const logSuccess = vi.fn();
      const logError = vi.fn();
      const { result: logResult } = renderHook(() => useLogTelephoneFollowUp(logSuccess, logError), { wrapper });
      await act(async () => {
        await logResult.current.mutateAsync({ requestId: 'r1', notes: 'noted' });
      });
      expect(api.logTelephoneFollowUp).toHaveBeenCalledWith('r1', 'noted');
      expect(logSuccess).toHaveBeenCalled();

      vi.mocked(api.logTelephoneFollowUp).mockRejectedValueOnce(new Error('Log fail'));
      await act(async () => {
        try {
          await logResult.current.mutateAsync({ requestId: 'r1', notes: 'noted' });
        } catch {}
      });
      expect(logError).toHaveBeenCalled();

      const tagSuccess = vi.fn();
      const tagError = vi.fn();
      const { result: tagResult } = renderHook(() => useAttachDelayTag(tagSuccess, tagError), { wrapper });
      await act(async () => {
        await tagResult.current.mutateAsync({ requestId: 'r1', data: {} as any });
      });
      expect(api.attachDelayTag).toHaveBeenCalledWith('r1', {});
      expect(tagSuccess).toHaveBeenCalled();

      vi.mocked(api.attachDelayTag).mockRejectedValueOnce(new Error('Tag fail'));
      await act(async () => {
        try {
          await tagResult.current.mutateAsync({ requestId: 'r1', data: {} as any });
        } catch {}
      });
      expect(tagError).toHaveBeenCalled();
    });

    it('useReferSisterHospital and useUpdateBmuConfig execute properly', async () => {
      const { wrapper } = createWrapper();
      const refSuccess = vi.fn();
      const { result: refResult } = renderHook(() => useReferSisterHospital(refSuccess), { wrapper });
      await act(async () => {
        await refResult.current.mutateAsync({} as any);
      });
      expect(api.referToSisterHospital).toHaveBeenCalled();
      expect(refSuccess).toHaveBeenCalled();

      const cfgSuccess = vi.fn();
      const { result: cfgResult } = renderHook(() => useUpdateBmuConfig(cfgSuccess), { wrapper });
      await act(async () => {
        await cfgResult.current.mutateAsync({} as any);
      });
      expect(api.updateConfig).toHaveBeenCalled();
      expect(cfgSuccess).toHaveBeenCalled();
    });

    it('useCheckinPatient, useVacatePatient, useCleanBed execute properly', async () => {
      const { wrapper } = createWrapper();
      const checkinSuccess = vi.fn();
      const { result: cResult } = renderHook(() => useCheckinPatient(checkinSuccess), { wrapper });
      await act(async () => {
        await cResult.current.mutateAsync('bed-1');
      });
      expect(api.checkinPatient).toHaveBeenCalledWith('bed-1');
      expect(checkinSuccess).toHaveBeenCalled();

      const vacateSuccess = vi.fn();
      const { result: vResult } = renderHook(() => useVacatePatient(vacateSuccess), { wrapper });
      await act(async () => {
        await vResult.current.mutateAsync('bed-1');
      });
      expect(api.vacatePatient).toHaveBeenCalledWith('bed-1');
      expect(vacateSuccess).toHaveBeenCalled();

      const cleanSuccess = vi.fn();
      const { result: clResult } = renderHook(() => useCleanBed(cleanSuccess), { wrapper });
      await act(async () => {
        await clResult.current.mutateAsync('bed-1');
      });
      expect(api.cleanBed).toHaveBeenCalledWith('bed-1');
      expect(cleanSuccess).toHaveBeenCalled();

      const simSuccess = vi.fn();
      const { result: simResult } = renderHook(() => useSimulatePeriodicUpdate(simSuccess), { wrapper });
      await act(async () => {
        await simResult.current.mutateAsync();
      });
      expect(simSuccess).toHaveBeenCalled();

      const { result: recResult } = renderHook(() => useRecordPatientAction(), { wrapper });
      await act(async () => {
        await recResult.current.mutateAsync({ token: 't1', actionType: 'MSW_CALL' });
      });
    });
  });
});
