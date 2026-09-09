import axios from 'axios';
import type {
  AdmissionRequest,
  AssessmentBroadcast,
  Bed,
  BedAllocationRequest,
  BedRecommendation,
  BmuAlgorithmConfig,
  BmuConfigUpdateRequest,
  DiversionReferralRequest,
  EdAssessmentSubmitRequest,
  Patient,
  PatientMilestoneResponse,
  RolePersona,
  SisterHospitalReferralResponse,
  SpecialistConsultRequest,
  SpecialtyCluster,
  Ward,
} from '../types/admissions';

const ROLE_STORAGE_KEY = 'admissions_role_persona';

export function getActiveRole(): RolePersona {
  return (typeof window !== 'undefined' && (localStorage.getItem(ROLE_STORAGE_KEY) as RolePersona)) || 'ED_ATTENDING';
}

export function setActiveRole(role: RolePersona) {
  if (typeof window !== 'undefined') localStorage.setItem(ROLE_STORAGE_KEY, role);
}

// Axios natively uses xsrfCookieName: 'XSRF-TOKEN' and xsrfHeaderName: 'X-XSRF-TOKEN' by default
const http = axios.create({
  withCredentials: true,
});

http.interceptors.request.use((config) => {
  config.headers.set('X-User-Role', getActiveRole());
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const problem = error.response?.data;
    if (problem?.detail) {
      error.message = problem.detail;
    } else if (problem?.title) {
      error.message = problem.title;
    }
    return Promise.reject(error);
  }
);

export const api = {
  // Auth & Session
  getAuthStatus: () => http.get<{ authenticated: boolean; username?: string; roles?: string[] }>('/api/v1/auth/me').then((r) => r.data),

  // Clinician Controller
  getEdWaitingPatients: () => http.get<Patient[]>('/api/v1/clinicians/ed/patients').then((r) => r.data),
  getEdSubmittedAdmissions: () => http.get<AdmissionRequest[]>('/api/v1/clinicians/ed/admissions').then((r) => r.data),
  submitEdAssessment: (data: EdAssessmentSubmitRequest) =>
    http.post<AdmissionRequest>('/api/v1/clinicians/ed/assessments/submit', data).then((r) => r.data),
  getSpecialistBroadcasts: (cluster?: SpecialtyCluster) =>
    http.get<AssessmentBroadcast[]>('/api/v1/clinicians/specialist/broadcasts', { params: cluster ? { cluster } : {} }).then((r) => r.data),
  claimBroadcast: (id: string) =>
    http.post<AssessmentBroadcast>(`/api/v1/clinicians/specialist/broadcasts/${id}/claim`).then((r) => r.data),
  submitConsult: (id: string, data: SpecialistConsultRequest) =>
    http.post<AssessmentBroadcast>(`/api/v1/clinicians/specialist/broadcasts/${id}/consult`, data).then((r) => r.data),

  // BMU Controller
  getPrioritizedQueue: () => http.get<AdmissionRequest[]>('/api/v1/bmu/queue').then((r) => r.data),
  getRecommendations: (requestId: string) =>
    http.get<BedRecommendation[]>(`/api/v1/bmu/recommendations/${requestId}`).then((r) => r.data),
  allocateBed: (data: BedAllocationRequest) =>
    http.post<AdmissionRequest>('/api/v1/bmu/allocate', data).then((r) => r.data),
  getInventory: () => http.get<Ward[]>('/api/v1/bmu/inventory').then((r) => r.data),
  getConfig: () => http.get<BmuAlgorithmConfig>('/api/v1/bmu/config').then((r) => r.data),
  updateConfig: (data: BmuConfigUpdateRequest) =>
    http.put<BmuAlgorithmConfig>('/api/v1/bmu/config', data).then((r) => r.data),
  referToSisterHospital: (data: DiversionReferralRequest) =>
    http.post<SisterHospitalReferralResponse>('/api/v1/bmu/diversion/refer', data).then((r) => r.data),

  // Patient & Turnover Controller
  trackPatient: (token: string) =>
    http.get<PatientMilestoneResponse>(`/api/v1/patients/track/${token}`).then((r) => r.data),
  getAvailablePatients: () => http.get<Patient[]>('/api/v1/patients/tokens').then((r) => r.data),
  checkinPatient: (bedId: string) =>
    http.post<Bed>(`/api/v1/patients/beds/${bedId}/checkin`).then((r) => r.data),
  vacatePatient: (bedId: string) =>
    http.post<Bed>(`/api/v1/patients/beds/${bedId}/vacate`).then((r) => r.data),
  cleanBed: (bedId: string) =>
    http.post<Bed>(`/api/v1/patients/beds/${bedId}/clean`).then((r) => r.data),
};
