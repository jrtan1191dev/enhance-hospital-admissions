import { describe, it, expect, beforeEach, vi } from 'vitest';

// We want to test interceptors and role functions
describe('services/api', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.resetModules();
  });

  it('getActiveRole returns ED_ATTENDING when localStorage is empty', async () => {
    const { getActiveRole } = await import('./api');
    expect(getActiveRole()).toBe('ED_ATTENDING');
  });

  it('setActiveRole stores role in localStorage and getActiveRole retrieves it', async () => {
    const { getActiveRole, setActiveRole } = await import('./api');
    setActiveRole('BMU_OFFICER');
    expect(localStorage.getItem('admissions_role_persona')).toBe('BMU_OFFICER');
    expect(getActiveRole()).toBe('BMU_OFFICER');

    setActiveRole('SPECIALIST_ON_CALL');
    expect(getActiveRole()).toBe('SPECIALIST_ON_CALL');
  });

  it('verifies all API endpoint functions invoke axios correctly', async () => {
    const mockAxiosInstance = {
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() },
      },
      get: vi.fn().mockResolvedValue({ data: { test: 'get-data' } }),
      post: vi.fn().mockResolvedValue({ data: { test: 'post-data' } }),
      put: vi.fn().mockResolvedValue({ data: { test: 'put-data' } }),
    };

    vi.doMock('axios', () => ({
      default: {
        create: vi.fn(() => mockAxiosInstance),
      },
    }));

    const { api } = await import('./api');

    // Test auth
    await expect(api.getAuthStatus()).resolves.toEqual({ test: 'get-data' });
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/v1/auth/me');

    // Test clinician ED endpoints
    await expect(api.getEdWaitingPatients()).resolves.toEqual({ test: 'get-data' });
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/v1/clinicians/ed/patients');

    await expect(api.getEdSubmittedAdmissions()).resolves.toEqual({ test: 'get-data' });
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/v1/clinicians/ed/admissions');

    const edPayload: any = { patientId: 'p1', provisionalDiagnosis: 'Flu' };
    await expect(api.submitEdAssessment(edPayload)).resolves.toEqual({ test: 'post-data' });
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/v1/clinicians/ed/assessments/submit', edPayload);

    // Test specialist endpoints
    await expect(api.getSpecialistBroadcasts()).resolves.toEqual({ test: 'get-data' });
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/v1/clinicians/specialist/broadcasts', { params: {} });

    await expect(api.getSpecialistBroadcasts('CARDIOLOGY')).resolves.toEqual({ test: 'get-data' });
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/v1/clinicians/specialist/broadcasts', { params: { cluster: 'CARDIOLOGY' } });

    await expect(api.claimBroadcast('b1')).resolves.toEqual({ test: 'post-data' });
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/v1/clinicians/specialist/broadcasts/b1/claim');

    const consultData: any = { responseNotes: 'Consult note' };
    await expect(api.submitConsult('b1', consultData)).resolves.toEqual({ test: 'post-data' });
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/v1/clinicians/specialist/broadcasts/b1/consult', consultData);

    await expect(api.amendConsult('b1', consultData)).resolves.toEqual({ test: 'put-data' });
    expect(mockAxiosInstance.put).toHaveBeenCalledWith('/api/v1/clinicians/specialist/broadcasts/b1/consult', consultData);

    const chainData: any = { targetCluster: 'GENERAL_MEDICINE', handoverNotes: 'notes' };
    await expect(api.chainConsult('b1', chainData)).resolves.toEqual({ test: 'post-data' });
    expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/v1/clinicians/specialist/broadcasts/b1/chain', chainData);

    // Test BMU endpoints
    await expect(api.getPrioritizedQueue()).resolves.toEqual({ test: 'get-data' });
    await expect(api.assignAdmittingCluster('req1', 'CARDIOLOGY')).resolves.toEqual({ test: 'post-data' });
    await expect(api.requestReconciliation('req1')).resolves.toEqual({ test: 'post-data' });
    await expect(api.getRecommendations('req1')).resolves.toEqual({ test: 'get-data' });
    await expect(api.allocateBed({ admissionRequestId: 'r1', bedId: 'b1' } as any)).resolves.toEqual({ test: 'post-data' });
    await expect(api.deallocateBed('r1')).resolves.toEqual({ test: 'post-data' });
    await expect(api.getInventory()).resolves.toEqual({ test: 'get-data' });
    await expect(api.getConfig()).resolves.toEqual({ test: 'get-data' });
    await expect(api.updateConfig({} as any)).resolves.toEqual({ test: 'put-data' });
    await expect(api.referToSisterHospital({} as any)).resolves.toEqual({ test: 'post-data' });
    await expect(api.getBatchSuggestions()).resolves.toEqual({ test: 'get-data' });
    await expect(api.approveBatchHoldingWard({} as any)).resolves.toEqual({ test: 'post-data' });
    await expect(api.getCohortSwapSuggestions()).resolves.toEqual({ test: 'get-data' });
    await expect(api.approveCohortSwap({} as any)).resolves.toEqual({ test: 'post-data' });
    await expect(api.recallDiversion('r1')).resolves.toEqual({ test: 'post-data' });
    await expect(api.extendDiversionSla('r1')).resolves.toEqual({ test: 'post-data' });
    await expect(api.logTelephoneFollowUp('r1', 'Follow up notes')).resolves.toEqual({ test: 'post-data' });
    await expect(api.attachDelayTag('r1', {} as any)).resolves.toEqual({ test: 'post-data' });

    // Test Patient & Turnover endpoints
    await expect(api.trackPatient('token123')).resolves.toEqual({ test: 'get-data' });
    expect(mockAxiosInstance.get).toHaveBeenCalledWith('/api/v1/patients/track/token123');

    await expect(api.getAvailablePatients()).resolves.toEqual({ test: 'get-data' });
    await expect(api.checkinPatient('bed1')).resolves.toEqual({ test: 'post-data' });
    await expect(api.vacatePatient('bed1')).resolves.toEqual({ test: 'post-data' });
    await expect(api.cleanBed('bed1')).resolves.toEqual({ test: 'post-data' });
    await expect(api.simulatePeriodicUpdate()).resolves.toEqual({ test: 'post-data' });
    await expect(api.recordPatientAction('token123', 'MSW_CALL')).resolves.toEqual({ test: 'post-data' });
  });

  it('tests axios interceptor logic (request header and response problem handling)', async () => {
    let reqInterceptor: ((config: any) => any) | undefined;
    let resSuccessInterceptor: ((res: any) => any) | undefined;
    let resErrorInterceptor: ((err: any) => any) | undefined;

    const mockAxiosInstance = {
      interceptors: {
        request: {
          use: vi.fn((fn) => {
            reqInterceptor = fn;
          }),
        },
        response: {
          use: vi.fn((successFn, errorFn) => {
            resSuccessInterceptor = successFn;
            resErrorInterceptor = errorFn;
          }),
        },
      },
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
    };

    vi.doMock('axios', () => ({
      default: {
        create: vi.fn(() => mockAxiosInstance),
      },
    }));

    const { setActiveRole } = await import('./api');
    setActiveRole('WARD_NURSE');

    // Verify request interceptor adds X-User-Role header
    const mockHeaders = new Map<string, string>();
    const mockConfig = {
      headers: {
        set: (k: string, v: string) => mockHeaders.set(k, v),
      },
    };
    reqInterceptor!(mockConfig);
    expect(mockHeaders.get('X-User-Role')).toBe('WARD_NURSE');

    // Verify response success interceptor passes response through
    const dummyResponse = { status: 200, data: 'ok' };
    expect(resSuccessInterceptor!(dummyResponse)).toBe(dummyResponse);

    // Verify response error with problem detail
    const errWithDetail: any = {
      response: { data: { detail: 'Specific problem detail error' } },
      message: 'Original message',
    };
    await expect(resErrorInterceptor!(errWithDetail)).rejects.toMatchObject({
      message: 'Specific problem detail error',
    });

    // Verify response error with problem title only
    const errWithTitle: any = {
      response: { data: { title: 'Problem Title' } },
      message: 'Original message',
    };
    await expect(resErrorInterceptor!(errWithTitle)).rejects.toMatchObject({
      message: 'Problem Title',
    });

    // Verify response error without problem details preserves original message
    const errGeneric: any = {
      response: { data: {} },
      message: 'Original network failure',
    };
    await expect(resErrorInterceptor!(errGeneric)).rejects.toMatchObject({
      message: 'Original network failure',
    });
  });
});
