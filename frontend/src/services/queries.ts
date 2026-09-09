import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from './api';
import type {
  BedAllocationRequest,
  BmuConfigUpdateRequest,
  DiversionReferralRequest,
  EdAssessmentSubmitRequest,
  SpecialistConsultRequest,
  SpecialtyCluster,
} from '../types/admissions';

// ============================================================================
// 1. TanStack Query Options (Encapsulated Keys, Fetchers & Polling Rules)
// ============================================================================

export const authQueries = {
  all: () => ['auth'] as const,
  me: () =>
    queryOptions({
      queryKey: [...authQueries.all(), 'me'] as const,
      queryFn: api.getAuthStatus,
    }),
};

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

export const specialistQueries = {
  all: () => ['specialist'] as const,
  broadcasts: (cluster?: SpecialtyCluster) =>
    queryOptions({
      queryKey: [...specialistQueries.all(), 'broadcasts', cluster ?? 'ALL'] as const,
      queryFn: () => api.getSpecialistBroadcasts(cluster),
      refetchInterval: 3000,
    }),
};

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
};

export const patientQueries = {
  all: () => ['patient'] as const,
  track: (token: string) =>
    queryOptions({
      queryKey: [...patientQueries.all(), 'track', token] as const,
      queryFn: () => api.trackPatient(token),
      refetchInterval: 2500,
      enabled: !!token,
    }),
  availablePatients: () =>
    queryOptions({
      queryKey: [...patientQueries.all(), 'available'] as const,
      queryFn: api.getAvailablePatients,
    }),
};

// ============================================================================
// 2. Centralized Mutation Hooks with Automatic Cache Invalidation
// ============================================================================

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

export function useReferSisterHospital(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: DiversionReferralRequest) => api.referToSisterHospital(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.queue().queryKey });
      onSuccess?.();
    },
  });
}

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

export function useCheckinPatient(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (bedId: string) => api.checkinPatient(bedId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.inventory().queryKey });
      queryClient.invalidateQueries({ queryKey: patientQueries.all() });
      onSuccess?.();
    },
  });
}

export function useVacatePatient(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (bedId: string) => api.vacatePatient(bedId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.all() });
      queryClient.invalidateQueries({ queryKey: patientQueries.all() });
      onSuccess?.();
    },
  });
}

export function useCleanBed(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (bedId: string) => api.cleanBed(bedId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bmuQueries.all() });
      onSuccess?.();
    },
  });
}
