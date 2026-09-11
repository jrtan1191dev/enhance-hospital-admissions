/**
 * Clinical triage acuity tier representing patient severity and priority.
 */
export type AcuityTier = 
  | 'TIER_1_CRITICAL'
  | 'TIER_2_ACUTE_URGENT'
  | 'TIER_3_ACUTE_STABLE'
  | 'TIER_4_SUBACUTE_DIVERSION'
  | 'TIER_5_OBSERVATION';

/**
 * Hospital clinical department or specialty cluster.
 */
export type SpecialtyCluster = 
  | 'CARDIOLOGY'
  | 'GENERAL_MEDICINE'
  | 'SURGERY'
  | 'ORTHOPAEDICS';

/**
 * Inpatient accommodation ward class tier.
 */
export type WardClass = 'A' | 'B1' | 'B2' | 'C';

/**
 * Biological gender used for room cohorting.
 */
export type Gender = 'MALE' | 'FEMALE';

/**
 * Infection control isolation classification.
 */
export type InfectionStatus = 
  | 'NONE'
  | 'CONTACT_MRSA'
  | 'AIRBORNE_COVID'
  | 'DROPLET';

/**
 * Operational bed state and color-coded status.
 */
export type BedStatus = 
  | 'EMPTY_PENDING_CLEANING' // MUSTARD YELLOW (Discharged, vacated, 30m cleaning pending)
  | 'EMPTY_CLEANED'          // WHITE (Cleaned, ready for assignment)
  | 'EMPTY_ASSIGNED'         // GREEN (Allocated by BMU, patient in transit)
  | 'OCCUPIED_TAKEN';        // GREY (Patient physically admitted/occupied)

/**
 * End-to-end lifecycle status of an admission request.
 */
export type AdmissionStatus = 
  | 'ASSESSMENT_PENDING'
  | 'BED_REQUESTED'
  | 'BED_ALLOCATED'
  | 'ADMITTED_INPATIENT'
  | 'DISCHARGED'
  | 'DIVERTED_HAH';

/**
 * Status of a specialist assessment broadcast.
 */
export type BroadcastStatus = 'OPEN' | 'CLAIMED' | 'AUTO_ESCALATED' | 'COMPLETED';

/**
 * Care diversion options to alternative facilities or home hospitalization.
 */
export type DiversionPathway = 'NONE' | 'COMMUNITY_HOSPITAL' | 'HOSPITAL_AT_HOME_MIC';

/**
 * Patient demographic and clinical baseline information.
 */
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

/**
 * Physical bed representation with capabilities and current occupant.
 */
export interface Bed {
  id: string;
  bedNumber: string;
  status: BedStatus;
  telemetryCapable: boolean;
  nearNursingStation: boolean;
  ward?: Ward;
  assignedPatient?: Patient;
}

/**
 * Hospital ward unit details including specialty alignment and room locks.
 */
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

/**
 * Clinical baseline field override entered during emergency department triage.
 */
export interface ClinicalBaselineOverride {
  field: string;
  originalValue: string;
  submittedValue: string;
  overrideReason?: string;
}

/**
 * Request payload for submitting an ED clinical admission assessment.
 */
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

/**
 * Inpatient admission request model capturing the entire clinical and operational flow.
 */
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

/**
 * Specialist assessment broadcast record for collaborative triage reviews.
 */
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

/**
 * Optimization weights and parameters used by BMU solver algorithms.
 */
export interface BmuAlgorithmConfig {
  id: string;
  weightSpecialtyCluster: number;
  weightConsolidation: number;
  weightFallRiskStation: number;
  batchHoldingWardThreshold: number;
}

/**
 * Update request payload for BMU solver weights.
 */
export interface BmuConfigUpdateRequest {
  weightSpecialtyCluster: number;
  weightConsolidation: number;
  weightFallRiskStation: number;
  batchHoldingWardThreshold: number;
}

/**
 * Ranked bed allocation recommendation generated for an admission request.
 */
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

/**
 * Real-time queue and journey status response provided to the patient tracker portal.
 */
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

/**
 * Confidence level of the Estimated Date of Discharge (EDD).
 */
export type EddConfidence = 'HIGH' | 'MEDIUM' | 'LOW';

/**
 * Discharge runway readiness milestone stages.
 */
export type DischargeRunwayStage =
  | 'RUNWAY_D3'
  | 'RUNWAY_D2'
  | 'RUNWAY_D1'
  | 'READY_FOR_MORNING_SIGNOFF'
  | 'MEDICATIONS_PENDING'
  | 'READY_TO_VACATE'
  | 'VACATED';

/**
 * Discharge medication delivery and dispensing status.
 */
export type MedicationDeliveryStatus =
  | 'NOT_DISPATCHED'
  | 'PACKING_IN_PROGRESS'
  | 'DELIVERED_BEDSIDE';

/**
 * SLA tracking status for 30-minute bed turnover sanitization.
 */
export type TurnoverSlaStatus = 'ON_TRACK' | 'APPROACHING_SLA' | 'BREACHED';

/**
 * Payload for setting or updating Estimated Date of Discharge.
 */
export interface EddUpdateRequest {
  edd: string;
  eddConfidence: EddConfidence;
  rationale?: string;
}

/**
 * Inpatient discharge runway record tracking readiness milestones.
 */
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

/**
 * Multi-day bed capacity projection forecast across wards and specialty clusters.
 */
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

/**
 * Bed cleaning and sanitization task tracking Environmental Services (EVS) SLAs.
 */
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

/**
 * Response payload confirming receipt and dispatch of an acute referral to a partner sister hospital.
 */
export interface SisterHospitalReferralResponse {
  referralId: string;
  destinationFacility: string;
  status: string;
  slaWindowMinutes: number;
  notes?: string;
}

/**
 * Payload for chaining an in-flight specialist consult to another clinical department.
 */
export interface ChainConsultRequest {
  targetCluster: SpecialtyCluster;
  rationale?: string;
}

/**
 * Specialist review and consult recommendation payload.
 */
export interface SpecialistConsultRequest {
  secondaryAcuityTier: AcuityTier;
  secondaryTelemetry?: boolean;
  consultNotes?: string;
  diversionRecommended: boolean;
  diversionPathway?: DiversionPathway;
}

/**
 * Request payload for allocating an inpatient bed to an admission request.
 */
export interface BedAllocationRequest {
  admissionRequestId: string;
  bedId: string;
  rank?: number;
  score?: number;
  overrideReason?: string;
}

/**
 * Request payload for referring an acute patient to an external diversion facility.
 */
export interface DiversionReferralRequest {
  admissionRequestId: string;
  facility: string;
}

/**
 * BMU solver suggestion for batch-allocating cohort-compatible patients to a holding ward.
 */
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

/**
 * Approval payload for executing a batch holding ward allocation.
 */
export interface BatchApprovalRequest {
  suggestionId: string;
  targetWardId: string;
  admissionRequestIds: string[];
}

/**
 * Suggestion to swap an existing inpatient's bed to release multi-bed capacity for pending admissions.
 */
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

/**
 * Approval payload for executing an inpatient cohort bed swap.
 */
export interface CohortSwapApprovalRequest {
  admissionRequestId: string;
  targetBedId: string;
}

/**
 * Simulated user persona role for testing and role-based interface views.
 */
export type RolePersona = 
  | 'ED_ATTENDING'
  | 'SPECIALIST'
  | 'BMU_COORDINATOR'
  | 'PATIENT'
  | 'WARD_NURSE'
  | 'HOUSEKEEPING';

/**
 * Standard delay reason categories for patient admissions and transfers.
 */
export type DelayReasonCode = 
  | 'HOUSEKEEPING_DELAY'
  | 'BED_SHORTAGE'
  | 'SPECIALIZED_ISOLATION_CLEANING'
  | 'SURGE_TRAUMA_EVENT';

/**
 * Payload for tagging an admission request with an operational delay code and explanation.
 */
export interface DelayTagRequest {
  delayReasonCode: DelayReasonCode;
  note?: string;
  operationalDelayReason?: string;
}

/**
 * Patient and family reassuring talking points corresponding to standard delay reason codes.
 */
export const DELAY_REASON_TALKING_POINTS: Record<DelayReasonCode, string> = {
  HOUSEKEEPING_DELAY: 'A bed in the assigned ward has been identified and our environmental services team is currently completing terminal cleaning and sanitization to ensure maximum patient safety.',
  BED_SHORTAGE: 'Hospital wards are currently experiencing high census. Our central bed management unit is actively reviewing bed turnover and prioritizing acute placement.',
  SPECIALIZED_ISOLATION_CLEANING: 'Your room requires specialized isolation infection-control protocols and bio-cleaning before safe transfer. Housekeeping is expediting this.',
  SURGE_TRAUMA_EVENT: 'The emergency department is managing a temporary acute surge in critical emergency admissions. Additional clinical staff and bed allocations are being mobilized.',
};

/**
 * Consolidated operational KPIs across ED intake, BMU capacity, patient tracking, and discharge.
 */
export interface HospitalKpiSummaryDto {
  periodStart: string;
  periodEnd: string;

  // Epic 1: Clinical Intake & Collaboration
  avgEdTurnaroundMinutes: number;
  edTurnaroundP95Minutes: number;
  specialistClaimLatencyAvgMinutes: number;
  primarySpecialistConcordanceRatePct: number;
  digitalBedRequestCount: number;

  // Epic 2: BMU Capacity & Diversions
  bmuSuggestionAcceptanceRatePct: number;
  bmuManualOverrideCount: number;
  totalDiversionCount: number;
  diversionRatePct: number;
  sisterHospitalSlaCompliancePct: number;
  batchHoldingWardAdoptionRatePct: number;
  overrideReasonsBreakdown?: Record<string, number>;
  diversionChannelsBreakdown?: Record<string, number>;

  // Epic 3: Patient Experience
  patientTrackerAccessRatePct: number;
  twoHourPeriodicUpdateDeliveryPct: number;
  prolongedWaitCommunicationRatePct: number;
  caregiverCounselingConnectRatePct: number;

  // Epic 4: Inpatient Discharge & Turnover
  dischargeBeforeNoonRatePct: number;
  advanceRunwayEstablishmentRatePct: number;
  bedsideMedicationDeliveryAdoptionPct: number;
  housekeepingTurnoverAvgMinutes: number;
  housekeeping30mSlaCompliancePct: number;
}
