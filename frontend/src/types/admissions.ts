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

export type WardClass = 
  | 'CLASS_A'
  | 'CLASS_B1'
  | 'CLASS_B2'
  | 'CLASS_C';

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
  | 'ASSESSMENT_PREPOPULATED'
  | 'PRIMARY_ASSESSMENT_SUBMITTED'
  | 'BED_REQUESTED'
  | 'BED_ALLOCATED'
  | 'DIVERTED_SISTER_HOSPITAL'
  | 'DIVERTED_HAH'
  | 'ADMITTED'
  | 'DISCHARGED';

export type BroadcastStatus = 'UNCLAIMED' | 'CLAIMED' | 'CONSULTED';

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

export interface AdmissionRequest {
  id: string;
  patient: Patient;
  primaryAcuityTier: AcuityTier;
  secondaryAcuityTier?: AcuityTier;
  discordant: boolean;
  status: AdmissionStatus;
  assignedBed?: Bed;
  createdAt: string;
  operationalDelayReason?: string;
}

export interface AssessmentBroadcast {
  id: string;
  admissionRequest: AdmissionRequest;
  cluster: SpecialtyCluster;
  status: BroadcastStatus;
  claimedByDoctor?: string;
  consultImpression?: string;
  recommendedAcuityTier?: AcuityTier;
  specialistDiversionEndorsed?: boolean;
}

export interface BmuAlgorithmConfig {
  id: string;
  specialtyMatchWeight: number;
  consolidationWeight: number;
  fallRiskProximityWeight: number;
  batchThreshold: number;
}

export interface BedRecommendation {
  bedId: string;
  bedNumber: string;
  wardCode: string;
  score: number;
  breakdown: string[];
  recommended: boolean;
}

export interface PatientMilestoneResponse {
  patientName: string;
  queueToken: string;
  milestoneNumber: number;
  milestoneLabel: string;
  estimatedWaitMinutes: number;
  paxAheadInQueue: number;
  assignedBedNumber?: string;
  assignedWardCode?: string;
  delayReason?: string;
  financialExplainer?: {
    coPayEstimate: string;
    rehabilitationPathway: string;
    mswContact: string;
  };
}

export interface SisterHospitalReferralResponse {
  referralId: string;
  facility: string;
  slaCountdownMinutes: number;
  status: string;
}

export interface EdAssessmentSubmitRequest {
  patientId: string;
  acuityTier: AcuityTier;
  specialtyCluster: SpecialtyCluster;
  wardClass: WardClass;
  telemetryRequired: boolean;
  fallRiskPrecautions: boolean;
  isolationRequired: InfectionStatus;
  clinicalNotes?: string;
}

export interface SpecialistConsultRequest {
  specialistId: string;
  consultImpression: string;
  recommendedAcuityTier: AcuityTier;
  specialistDiversionEndorsed: boolean;
}

export interface BedAllocationRequest {
  admissionRequestId: string;
  bedId: string;
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
