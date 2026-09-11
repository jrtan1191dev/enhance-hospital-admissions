import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from './api';
import type {
  BatchApprovalRequest,
  BedAllocationRequest,
  BmuConfigUpdateRequest,
  CohortSwapApprovalRequest,
  DelayTagRequest,
  DiversionReferralRequest,
  EdAssessmentSubmitRequest,
  SpecialistConsultRequest,
  SpecialtyCluster,
  EddUpdateRequest,
} from '../types/admissions';

// ============================================================================
// 1. TanStack Query Options (Encapsulated Keys, Fetchers & Polling Rules)
// ============================================================================

/**
 * Authentication and current user session queries.
 */
export const authQueries = {
  all: () => ['auth'] as const,
  me: () =>
    queryOptions({
      queryKey: [...authQueries.all(), 'me'] as const,
      queryFn: api.getAuthStatus,
    }),
};

/**
 * Emergency Department (ED) triage and admission queries.
 */
export const edQueries = {
  all: () => ['ed'] as const,
  patients: () =>
    queryOptions({
      queryKey: [...edQueries.all(), 'patients'] as const,
      queryFn: api.getEdWaitingPatients,
      refetchInterval: 3000,
    }),
  admissions: () =>
    queryOptions({
      queryKey: [...edQueries.all(), 'admissions'] as const,
      queryFn: api.getEdSubmittedAdmissions,
      refetchInterval: 3000,
    }),
};

/**
 * Specialist consult broadcast queries.
 */
export const specialistQueries = {
  all: () => ['specialist'] as const,
  broadcasts: (cluster?: SpecialtyCluster) =>
    queryOptions({
      queryKey: [...specialistQueries.all(), 'broadcasts', cluster ?? 'ALL'] as const,
      queryFn: () => api.getSpecialistBroadcasts(cluster),
      refetchInterval: 3000,
    }),
};

/**
 * Bed Management Unit (BMU) queue, recommendations, inventory, and solver suggestions queries.
 */
export const bmuQueries = {
  all: () => ['bmu'] as const,
  queue: () =>
    queryOptions({
      queryKey: [...bmuQueries.all(), 'queue'] as const,
      queryFn: api.getPrioritizedQueue,
      refetchInterval: 3000,
    }),
  inventory: () =>
    queryOptions({
      queryKey: [...bmuQueries.all(), 'inventory'] as const,
      queryFn: api.getInventory,
      refetchInterval: 3000,
    }),
  recommendations: (requestId?: string) =>
    queryOptions({
      queryKey: [...bmuQueries.all(), 'recommendations', requestId ?? 'none'] as const,
      queryFn: () => (requestId ? api.getRecommendations(requestId) : Promise.resolve([])),
      enabled: !!requestId,
    }),
  config: () =>
    queryOptions({
      queryKey: [...bmuQueries.all(), 'config'] as const,
      queryFn: api.getConfig,
    }),
  batchSuggestions: () =>
    queryOptions({
      queryKey: [...bmuQueries.all(), 'batchSuggestions'] as const,
      queryFn: api.getBatchSuggestions,
      refetchInterval: 3000,
    }),
  cohortSwapSuggestions: () =>
    queryOptions({
      queryKey: [...bmuQueries.all(), 'cohortSwapSuggestions'] as const,
      queryFn: api.getCohortSwapSuggestions,
      refetchInterval: 3000,
    }),
};

/**
 * Patient tracking and self-service status queries.
 */
export const patientQueries = {
  all: () => ['patient'] as const,
  track: (token: string) =>
    queryOptions({
      queryKey: [...patientQueries.all(), 'track', token] as const,
      queryFn: () => api.trackPatient(token),
      refetchInterval: 3000,
      enabled: !!token,
    }),
  availablePatients: () =>
    queryOptions({
      queryKey: [...patientQueries.all(), 'available'] as const,
      queryFn: api.getAvailablePatients,
    }),
};

/**
 * Inpatient ward runway, turnover tasks, and capacity forecast queries.
 */
export const wardQueries = {
  all: () => ['ward'] as const,
  runway: () =>
    queryOptions({
      queryKey: [...wardQueries.all(), 'runway'] as const,
      queryFn: api.getRunway,
      refetchInterval: 3000,
    }),
  capacityForecast: () =>
    queryOptions({
      queryKey: [...wardQueries.all(), 'capacity-forecast'] as const,
      queryFn: api.getCapacityForecast,
      refetchInterval: 3000,
    }),
  turnoverTasks: () =>
    queryOptions({
      queryKey: [...wardQueries.all(), 'turnover-tasks'] as const,
      queryFn: api.getTurnoverTasks,
      refetchInterval: 3000,
    }),
};

// ============================================================================
// 2. Centralized Mutation Hooks with Automatic Cache Invalidation
// ============================================================================

/**
 * React Query mutation hook to submit an emergency department admission assessment.
 * Automatically invalidates ED and BMU queue queries upon success.
 *
 * @param onSuccess - Optional callback triggered on success.
 */
export function useSubmitEdAssessment(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: EdAssessmentSubmitRequest) => api.submitEdAssessment(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: edQueries.all() });
      queryClient.invalidateQueries({ queryKey: bmuQueries.queue().queryKey });
      queryClient.invalidateQueries({ queryKey: specialistQueries.all() });
      onSuccess?.();
    },
  });
}

/**
 * React Query mutation hook for an on-call specialist to claim a consult broadcast.
 *
 * @param onSuccess - Optional callback triggered on success.
 * @param onError - Optional callback triggered on error.
 */
export function useClaimBroadcast(onSuccess?: () => void, onError?: (error: Error) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.claimBroadcast(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: specialistQueries.all() });
      onSuccess?.();
    },
    onError: (error: Error) => {
      queryClient.invalidateQueries({ queryKey: specialistQueries.all() });
      onError?.(error);
    },
  });
}

/**
 * React Query mutation hook for submitting specialist consult notes and secondary acuity.
 *
 * @param onSuccess - Optional callback triggered on success.
 */
export function useSubmitConsult(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variables: { id: string; data: SpecialistConsultRequest }) =>
      api.submitConsult(variables.id, variables.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: specialistQueries.all() });
      queryClient.invalidateQueries({ queryKey: bmuQueries.queue().queryKey });
      onSuccess?.();
    },
  });
}

/**
 * React Query mutation hook for amending previously submitted specialist consult findings.
 *
 * @param onSuccess - Optional callback triggered on success.
 * @param onError - Optional callback triggered on error.
 */
export function useAmendConsult(onSuccess?: () => void, onError?: (error: Error) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variables: { id: string; data: SpecialistConsultRequest }) =>
      api.amendConsult(variables.id, variables.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: specialistQueries.all() });
      queryClient.invalidateQueries({ queryKey: bmuQueries.queue().queryKey });
      onSuccess?.();
    },
    onError: (error: Error) => {
      queryClient.invalidateQueries({ queryKey: specialistQueries.all() });
      onError?.(error);
    },
  });
}

/**
 * React Query mutation hook for requesting clinical reconciliation between discordant assessments.
 *
 * @param onSuccess - Optional callback triggered on success.
 * @param onError - Optional callback triggered on error.
 */
export function useRequestReconciliation(onSuccess?: () => void, onError?: (error: Error) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (requestId: string) => api.requestReconciliation(requestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.queue().queryKey });
      queryClient.invalidateQueries({ queryKey: edQueries.all() });
      queryClient.invalidateQueries({ queryKey: specialistQueries.all() });
      onSuccess?.();
    },
    onError: (error: Error) => {
      onError?.(error);
    },
  });
}

/**
 * React Query mutation hook for assigning an admitting specialty cluster to an admission request.
 *
 * @param onSuccess - Optional callback triggered on success.
 * @param onError - Optional callback triggered on error.
 */
export function useAssignAdmittingCluster(onSuccess?: () => void, onError?: (error: Error) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variables: { requestId: string; cluster: SpecialtyCluster }) =>
      api.assignAdmittingCluster(variables.requestId, variables.cluster),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.all() });
      onSuccess?.();
    },
    onError: (error: Error) => {
      onError?.(error);
    },
  });
}

/**
 * React Query mutation hook for chaining a consult to an additional specialty cluster.
 *
 * @param onSuccess - Optional callback triggered on success.
 * @param onError - Optional callback triggered on error.
 */
export function useChainConsult(onSuccess?: () => void, onError?: (error: Error) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variables: { id: string; data: import('../types/admissions').ChainConsultRequest }) =>
      api.chainConsult(variables.id, variables.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: specialistQueries.all() });
      onSuccess?.();
    },
    onError: (error: Error) => {
      queryClient.invalidateQueries({ queryKey: specialistQueries.all() });
      onError?.(error);
    },
  });
}

/**
 * React Query mutation hook for BMU bed allocation.
 *
 * @param onSuccess - Optional callback triggered on success.
 */
export function useAllocateBed(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: BedAllocationRequest) => api.allocateBed(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.all() });
      queryClient.invalidateQueries({ queryKey: patientQueries.all() });
      onSuccess?.();
    },
  });
}

/**
 * React Query mutation hook for deallocating or releasing an assigned bed.
 *
 * @param onSuccess - Optional callback triggered on success.
 */
export function useDeallocateBed(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (admissionRequestId: string) => api.deallocateBed(admissionRequestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.all() });
      queryClient.invalidateQueries({ queryKey: patientQueries.all() });
      onSuccess?.();
    },
  });
}

/**
 * React Query mutation hook for approving a batch holding ward allocation.
 *
 * @param onSuccess - Optional callback triggered on success.
 * @param onError - Optional callback triggered on error.
 */
export function useApproveBatchHoldingWard(onSuccess?: () => void, onError?: (error: Error) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: BatchApprovalRequest) => api.approveBatchHoldingWard(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.all() });
      queryClient.invalidateQueries({ queryKey: patientQueries.all() });
      onSuccess?.();
    },
    onError: (error: Error) => {
      onError?.(error);
    },
  });
}

/**
 * React Query mutation hook for approving a patient cohort bed swap.
 *
 * @param onSuccess - Optional callback triggered on success.
 * @param onError - Optional callback triggered on error.
 */
export function useApproveCohortSwap(onSuccess?: () => void, onError?: (error: Error) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CohortSwapApprovalRequest) => api.approveCohortSwap(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.all() });
      queryClient.invalidateQueries({ queryKey: patientQueries.all() });
      onSuccess?.();
    },
    onError: (error: Error) => {
      onError?.(error);
    },
  });
}

/**
 * React Query mutation hook for referring an acute patient to a sister hospital.
 *
 * @param onSuccess - Optional callback triggered on success.
 */
export function useReferSisterHospital(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: DiversionReferralRequest) => api.referToSisterHospital(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.all() });
      onSuccess?.();
    },
  });
}

/**
 * React Query mutation hook for recalling a previously initiated diversion back to acute hospital care.
 *
 * @param onSuccess - Optional callback triggered on success.
 * @param onError - Optional callback triggered on error.
 */
export function useRecallDiversion(onSuccess?: () => void, onError?: (error: Error) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (requestId: string) => api.recallDiversion(requestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.all() });
      onSuccess?.();
    },
    onError: (error: Error) => {
      onError?.(error);
    },
  });
}

/**
 * React Query mutation hook for extending the sister hospital acceptance SLA window.
 *
 * @param onSuccess - Optional callback triggered on success.
 * @param onError - Optional callback triggered on error.
 */
export function useExtendDiversionSla(onSuccess?: () => void, onError?: (error: Error) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (requestId: string) => api.extendDiversionSla(requestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.all() });
      onSuccess?.();
    },
    onError: (error: Error) => {
      onError?.(error);
    },
  });
}

/**
 * React Query mutation hook for logging telephone counseling notes for waiting patients.
 *
 * @param onSuccess - Optional callback triggered on success.
 * @param onError - Optional callback triggered on error.
 */
export function useLogTelephoneFollowUp(onSuccess?: () => void, onError?: (error: Error) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variables: { requestId: string; notes: string }) =>
      api.logTelephoneFollowUp(variables.requestId, variables.notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.all() });
      onSuccess?.();
    },
    onError: (error: Error) => {
      onError?.(error);
    },
  });
}

/**
 * React Query mutation hook for tagging an admission request with an operational delay code and note.
 *
 * @param onSuccess - Optional callback triggered on success.
 * @param onError - Optional callback triggered on error.
 */
export function useAttachDelayTag(onSuccess?: () => void, onError?: (error: Error) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variables: { requestId: string; data: DelayTagRequest }) =>
      api.attachDelayTag(variables.requestId, variables.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.all() });
      queryClient.invalidateQueries({ queryKey: edQueries.all() });
      onSuccess?.();
    },
    onError: (error: Error) => {
      onError?.(error);
    },
  });
}

/**
 * React Query mutation hook for modifying dynamic BMU algorithm weights.
 *
 * @param onSuccess - Optional callback triggered on success.
 */
export function useUpdateBmuConfig(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: BmuConfigUpdateRequest) => api.updateConfig(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.config().queryKey });
      queryClient.invalidateQueries({ queryKey: [...bmuQueries.all(), 'recommendations'] });
      onSuccess?.();
    },
  });
}

/**
 * React Query mutation hook to record patient arrival and check-in to an inpatient bed.
 *
 * @param onSuccess - Optional callback triggered on success.
 */
export function useCheckinPatient(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (bedId: string) => api.checkinPatient(bedId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.inventory().queryKey });
      queryClient.invalidateQueries({ queryKey: patientQueries.all() });
      queryClient.invalidateQueries({ queryKey: wardQueries.all() });
      onSuccess?.();
    },
  });
}

/**
 * React Query mutation hook for vacating a bed when a patient is discharged, triggering cleaning SLA.
 *
 * @param onSuccess - Optional callback triggered on success.
 */
export function useVacatePatient(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (bedId: string) => api.vacatePatient(bedId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.all() });
      queryClient.invalidateQueries({ queryKey: patientQueries.all() });
      queryClient.invalidateQueries({ queryKey: wardQueries.all() });
      onSuccess?.();
    },
  });
}

/**
 * React Query mutation hook for completing bed sanitization by housekeeping staff.
 *
 * @param onSuccess - Optional callback triggered on success.
 * @param onError - Optional callback triggered on error.
 */
export function useCleanBed(onSuccess?: () => void, onError?: (err: Error) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (bedId: string) => api.cleanBed(bedId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.all() });
      queryClient.invalidateQueries({ queryKey: wardQueries.all() });
      onSuccess?.();
    },
    onError: (err) => {
      onError?.(err);
    },
  });
}

/**
 * React Query mutation hook for updating Estimated Date of Discharge (EDD).
 *
 * @param onSuccess - Optional callback triggered on success.
 * @param onError - Optional callback triggered on error.
 */
export function useUpdateEdd(onSuccess?: () => void, onError?: (err: Error) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variables: { patientId: string; data: EddUpdateRequest }) =>
      api.updateEdd(variables.patientId, variables.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: wardQueries.all() });
      queryClient.invalidateQueries({ queryKey: bmuQueries.all() });
      queryClient.invalidateQueries({ queryKey: patientQueries.all() });
      onSuccess?.();
    },
    onError: (err) => {
      onError?.(err);
    },
  });
}

/**
 * React Query mutation hook for attending physician morning discharge sign-off.
 *
 * @param onSuccess - Optional callback triggered on success.
 * @param onError - Optional callback triggered on error.
 */
export function useDischargeSignoff(onSuccess?: () => void, onError?: (err: Error) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (patientId: string) => api.dischargeSignoff(patientId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: wardQueries.all() });
      queryClient.invalidateQueries({ queryKey: patientQueries.all() });
      onSuccess?.();
    },
    onError: (err) => {
      onError?.(err);
    },
  });
}

/**
 * React Query mutation hook for recording bedside delivery of discharge medication.
 *
 * @param onSuccess - Optional callback triggered on success.
 * @param onError - Optional callback triggered on error.
 */
export function useDeliverMedication(onSuccess?: () => void, onError?: (err: Error) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (patientId: string) => api.deliverMedication(patientId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: wardQueries.all() });
      queryClient.invalidateQueries({ queryKey: patientQueries.all() });
      onSuccess?.();
    },
    onError: (err) => {
      onError?.(err);
    },
  });
}

/**
 * React Query mutation hook for triggering simulated 2-hour periodic SMS updates to patients.
 *
 * @param onSuccess - Optional callback triggered on success with dispatch counts.
 */
export function useSimulatePeriodicUpdate(onSuccess?: (data: { dispatchedCount: number; message: string }) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api.simulatePeriodicUpdate(),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: patientQueries.all() });
      onSuccess?.(data);
    },
  });
}

/**
 * React Query mutation hook for logging patient self-service call actions from the tracker.
 */
export function useRecordPatientAction() {
  return useMutation({
    mutationFn: (variables: { token: string; actionType: 'MSW_CALL' | 'FINANCE_CALL' }) =>
      api.recordPatientAction(variables.token, variables.actionType),
  });
}

/**
 * Analytics and operational KPI summary queries.
 */
export const analyticsQueries = {
  all: () => ['analytics'] as const,
  kpiSummary: (startDate?: string, endDate?: string) =>
    queryOptions({
      queryKey: [...analyticsQueries.all(), 'kpiSummary', { startDate, endDate }] as const,
      queryFn: () => api.getKpiSummary(startDate, endDate),
      refetchInterval: 5000,
    }),
};

