export type AcuityTier = 
  | 'TIER_1_CRITICAL'
  | 'TIER_2_ACUTE_URGENT'
  | 'TIER_3_ACUTE_STABLE'
  | 'TIER_4_SUBACUTE_DIVERSION'
  | 'TIER_5_OBSERVATION';

export type SpecialtyCluster = 
  | 'CARDIOLOGY'
  | 'GENERAL_MEDICINE'
  | 'SURGERY'
  | 'ORTHOPAEDICS';

export type WardClass = 'A' | 'B1' | 'B2' | 'C';

export type Gender = 'MALE' | 'FEMALE';

export type InfectionStatus = 
  | 'NONE'
  | 'CONTACT_MRSA'
  | 'AIRBORNE_COVID'
  | 'DROPLET';

export type BedStatus = 
  | 'EMPTY_PENDING_CLEANING' // MUSTARD YELLOW (Discharged, vacated, 30m cleaning pending)
  | 'EMPTY_CLEANED'          // WHITE (Cleaned, ready for assignment)
  | 'EMPTY_ASSIGNED'         // GREEN (Allocated by BMU, patient in transit)
  | 'OCCUPIED_TAKEN';        // GREY (Patient physically admitted/occupied)

export type AdmissionStatus = 
  | 'ASSESSMENT_PENDING'
  | 'BED_REQUESTED'
  | 'BED_ALLOCATED'
  | 'ADMITTED_INPATIENT'
  | 'DISCHARGED';

export type BroadcastStatus = 'OPEN' | 'CLAIMED' | 'AUTO_ESCALATED' | 'COMPLETED';

export type DiversionPathway = 'NONE' | 'COMMUNITY_HOSPITAL' | 'HOSPITAL_AT_HOME_MIC';

export interface Patient {
  id: string;
  name: string;
  nric: string;
  gender: Gender;
  age: number;
  queueToken: string;
  wardClassPreference: WardClass;
  infectionStatus: InfectionStatus;
  telemetryRequired: boolean;
  fallRiskScore: number;
  vitalsBp?: string;
  vitalsHr?: number;
  vitalsSpo2?: number;
  labTroponin?: string;
  labWbc?: string;
  suspectedDiagnosis?: string;
  waitingInEd?: boolean;
}

export interface Bed {
  id: string;
  bedNumber: string;
  status: BedStatus;
  telemetryCapable: boolean;
  nearNursingStation: boolean;
  ward?: Ward;
  assignedPatient?: Patient;
}

export interface Ward {
  id: string;
  wardCode: string;
  levelNumber: number;
  wardClass: WardClass;
  genderCohortLocked?: Gender;
  infectionLocked?: InfectionStatus;
  specialty: SpecialtyCluster;
  beds?: Bed[];
}

export interface ClinicalBaselineOverride {
  field: string;
  originalValue: string;
  submittedValue: string;
  overrideReason?: string;
}

export interface EdAssessmentSubmitRequest {
  patientId: string;
  suspectedDiagnosisService: SpecialtyCluster;
  primaryAcuityTier: AcuityTier;
  requestedWardClass: WardClass;
  needsTelemetry?: boolean;
  primaryTelemetry?: boolean;
  requiresSpecialistConsult?: boolean;
  targetClusters?: SpecialtyCluster[];
  overrides?: ClinicalBaselineOverride[];
  clinicalNotes?: string;
  recommendedAccepted?: boolean;
  elapsedMins?: number;
}

export interface AdmissionRequest {
  id: string;
  patient: Patient;
  suspectedDiagnosisService?: SpecialtyCluster;
  primaryAcuityTier: AcuityTier;
  secondaryAcuityTier?: AcuityTier;
  effectiveAcuityTier?: AcuityTier;
  primaryTelemetry?: boolean;
  secondaryTelemetry?: boolean;
  effectiveTelemetry?: boolean;
  requestedWardClass?: WardClass;
  requiresSpecialistConsult?: boolean;
  reconciliationRequested?: boolean;
  admittingSpecialtyCluster?: SpecialtyCluster;
  discordant: boolean;
  status: AdmissionStatus;
  assignedBed?: Bed;
  requestedAt?: string;
  createdAt: string;
  operationalDelayReason?: string;
  diversionRecommended?: boolean;
  diversionPathway?: DiversionPathway;
  sisterHospitalReferralId?: string;
  isRecommendationAccepted?: boolean;
  overrideReasonCode?: string;
  delayReasonTag?: string;
}

export interface AssessmentBroadcast {
  id: string;
  admissionRequest: AdmissionRequest;
  targetCluster: SpecialtyCluster;
  status: BroadcastStatus;
  claimedBySpecialistId?: string;
  claimedAt?: string;
  consultNotes?: string;
  secondaryAcuityTier?: AcuityTier;
  secondaryTelemetry?: boolean;
  diversionPathway?: DiversionPathway;
  parentBroadcastId?: string;
}

export interface BmuAlgorithmConfig {
  id: string;
  weightSpecialtyCluster: number;
  weightConsolidation: number;
  weightFallRiskStation: number;
  batchHoldingWardThreshold: number;
}

export interface BmuConfigUpdateRequest {
  weightSpecialtyCluster: number;
  weightConsolidation: number;
  weightFallRiskStation: number;
  batchHoldingWardThreshold: number;
}

export interface BedRecommendation {
  bedId: string;
  bedNumber: string;
  level: number;
  wardName: string;
  score: number;
  scoreBreakdown: string[];
  isRecommended: boolean;
}

export interface PatientMilestoneResponse {
  patientId?: string;
  patientName: string;
  queueToken: string;
  admissionStatus: AdmissionStatus;
  queuePosition: number;
  estimatedWaitMinutes: number;
  assignedBedNumber?: string;
  assignedWardName?: string;
  assignedLevel?: number;
  coPayEstimate?: string;
  careGuidance?: string;
}

export interface SisterHospitalReferralResponse {
  referralId: string;
  destinationFacility: string;
  status: string;
  slaWindowMinutes: number;
  notes?: string;
}


export interface ChainConsultRequest {
  targetCluster: SpecialtyCluster;
  rationale?: string;
}

export interface SpecialistConsultRequest {
  secondaryAcuityTier: AcuityTier;
  secondaryTelemetry?: boolean;
  consultNotes?: string;
  diversionRecommended: boolean;
  diversionPathway?: DiversionPathway;
}

export interface BedAllocationRequest {
  admissionRequestId: string;
  bedId: string;
  rank?: number;
  score?: number;
  overrideReason?: string;
}

export interface DiversionReferralRequest {
  admissionRequestId: string;
  facility: string;
}

export type RolePersona = 
  | 'ED_ATTENDING'
  | 'SPECIALIST'
  | 'BMU_COORDINATOR'
  | 'PATIENT'
  | 'WARD_NURSE'
  | 'HOUSEKEEPING';
