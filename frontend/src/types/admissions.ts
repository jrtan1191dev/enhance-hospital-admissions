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
  | 'DISCHARGED'
  | 'DIVERTED_HAH';

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
  clinicalConditionUpdated?: boolean;
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
  archivedDelayReasonTag?: string;
  archivedOperationalDelayReason?: string;
  referralDispatchedAt?: string;
  referralSlaMinutes?: number;
  referralFacility?: string;
  virtualBedNumber?: string;
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
  createdAt?: string;
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
  isSafetyViolated?: boolean;
  safetyViolationReason?: string;
  isOperationalOverride?: boolean;
  operationalOverrideReason?: string;
}

export interface PatientMilestoneResponse {
  patientId?: string;
  patientName: string;
  queueToken: string;
  admissionStatus: AdmissionStatus;
  requestedWardClass?: WardClass;
  queuePosition: number;
  patientsAhead?: number;
  estimatedWaitMinutes: number;
  assignedBedNumber?: string;
  assignedWardName?: string;
  assignedLevel?: number;
  delayReason?: string;
  delayContactHotline?: string;
  coPayEstimate?: string;
  careGuidance?: string;
  diversionRecommended?: boolean;
  diversionPathway?: DiversionPathway;
  estimatedDateOfDischarge?: string;
  eddConfidence?: EddConfidence;
  medicationDeliveryStatus?: MedicationDeliveryStatus;
  runwayStage?: DischargeRunwayStage;
}

export type EddConfidence = 'HIGH' | 'MEDIUM' | 'LOW';

export type DischargeRunwayStage =
  | 'RUNWAY_D3'
  | 'RUNWAY_D2'
  | 'RUNWAY_D1'
  | 'READY_FOR_MORNING_SIGNOFF'
  | 'MEDICATIONS_PENDING'
  | 'READY_TO_VACATE'
  | 'VACATED';

export type MedicationDeliveryStatus =
  | 'NOT_DISPATCHED'
  | 'PACKING_IN_PROGRESS'
  | 'DELIVERED_BEDSIDE';

export type TurnoverSlaStatus = 'ON_TRACK' | 'APPROACHING_SLA' | 'BREACHED';

export interface EddUpdateRequest {
  edd: string;
  eddConfidence: EddConfidence;
  rationale?: string;
}

export interface DischargeRunwayDto {
  patientId: string;
  patientName: string;
  bedId?: string;
  bedNumber: string;
  wardCode: string;
  levelNumber?: number;
  estimatedDateOfDischarge?: string;
  confidence?: EddConfidence;
  runwayStage?: DischargeRunwayStage;
  dischargeSignoffAt?: string;
  medicationStatus: MedicationDeliveryStatus;
  rationale?: string;
}

export interface BmuCapacityForecastDto {
  totalNext24Hours: number;
  totalNext48Hours: number;
  totalNext72Hours: number;
  byWard: Array<{
    wardCode: string;
    cluster?: SpecialtyCluster;
    next24Hours: number;
    next48Hours: number;
    next72Hours: number;
    total: number;
  }>;
  byCluster: Array<{
    cluster: SpecialtyCluster;
    next24Hours: number;
    next48Hours: number;
    next72Hours: number;
    total: number;
  }>;
}

export interface TurnoverTaskDto {
  bedId: string;
  bedNumber: string;
  wardCode: string;
  levelNumber?: number;
  vacatedAt: string;
  cleaningStartedAt: string;
  remainingMinutes: number;
  slaStatus: TurnoverSlaStatus;
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

export interface BatchSuggestion {
  suggestionId: string;
  targetWardId: string;
  targetWardName: string;
  patientIds: string[];
  patientNames: string[];
  admissionRequestIds: string[];
  commonWardClass: WardClass;
  commonGender: Gender;
  commonInfectionStatus: InfectionStatus;
  unlockedCapacityCount: number;
}

export interface BatchApprovalRequest {
  suggestionId: string;
  targetWardId: string;
  admissionRequestIds: string[];
}

export interface CohortSwapSuggestion {
  suggestionId: string;
  admissionRequestId: string;
  patientId: string;
  patientName: string;
  patientGender: Gender;
  patientWardClass: WardClass;
  currentBedId: string;
  currentBedNumber: string;
  currentWardId: string;
  currentWardName: string;
  targetBedId: string;
  targetBedNumber: string;
  targetWardId: string;
  targetWardName: string;
  unlockedCapacityCount: number;
}

export interface CohortSwapApprovalRequest {
  admissionRequestId: string;
  targetBedId: string;
}

export type RolePersona = 
  | 'ED_ATTENDING'
  | 'SPECIALIST'
  | 'BMU_COORDINATOR'
  | 'PATIENT'
  | 'WARD_NURSE'
  | 'HOUSEKEEPING';

export type DelayReasonCode = 
  | 'HOUSEKEEPING_DELAY'
  | 'BED_SHORTAGE'
  | 'SPECIALIZED_ISOLATION_CLEANING'
  | 'SURGE_TRAUMA_EVENT';

export interface DelayTagRequest {
  delayReasonCode: DelayReasonCode;
  note?: string;
  operationalDelayReason?: string;
}

export const DELAY_REASON_TALKING_POINTS: Record<DelayReasonCode, string> = {
  HOUSEKEEPING_DELAY: 'A bed in the assigned ward has been identified and our environmental services team is currently completing terminal cleaning and sanitization to ensure maximum patient safety.',
  BED_SHORTAGE: 'Hospital wards are currently experiencing high census. Our central bed management unit is actively reviewing bed turnover and prioritizing acute placement.',
  SPECIALIZED_ISOLATION_CLEANING: 'Your room requires specialized isolation infection-control protocols and bio-cleaning before safe transfer. Housekeeping is expediting this.',
  SURGE_TRAUMA_EVENT: 'The emergency department is managing a temporary acute surge in critical emergency admissions. Additional clinical staff and bed allocations are being mobilized.',
};
