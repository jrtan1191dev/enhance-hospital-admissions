package com.hospital.admissions.domain;

import com.hospital.admissions.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainAndDtoTest {

    @Test
    @DisplayName("Verify Enums coverage")
    void testEnums() {
        assertThat(AcuityTier.values()).contains(
                AcuityTier.TIER_1_CRITICAL,
                AcuityTier.TIER_2_ACUTE_URGENT,
                AcuityTier.TIER_3_ACUTE_STABLE,
                AcuityTier.TIER_4_SUBACUTE_DIVERSION,
                AcuityTier.TIER_5_SHORT_STAY
        );

        assertThat(AdmissionStatus.values()).contains(
                AdmissionStatus.ASSESSMENT_PENDING,
                AdmissionStatus.BED_REQUESTED,
                AdmissionStatus.BED_ALLOCATED,
                AdmissionStatus.ADMITTED_INPATIENT,
                AdmissionStatus.DISCHARGED
        );

        assertThat(BedStatus.values()).contains(
                BedStatus.EMPTY_CLEANED,
                BedStatus.EMPTY_ASSIGNED,
                BedStatus.OCCUPIED_TAKEN,
                BedStatus.EMPTY_PENDING_CLEANING
        );

        assertThat(BroadcastStatus.values()).contains(
                BroadcastStatus.OPEN,
                BroadcastStatus.CLAIMED,
                BroadcastStatus.COMPLETED
        );

        assertThat(Gender.values()).contains(Gender.MALE, Gender.FEMALE);

        assertThat(InfectionStatus.values()).contains(
                InfectionStatus.NON_INFECTIOUS,
                InfectionStatus.RESPIRATORY,
                InfectionStatus.MRSA
        );

        assertThat(SpecialtyCluster.values()).contains(
                SpecialtyCluster.CARDIOLOGY,
                SpecialtyCluster.GENERAL_MEDICINE,
                SpecialtyCluster.SURGERY,
                SpecialtyCluster.ORTHOPAEDICS
        );

        assertThat(WardClass.values()).contains(
                WardClass.A,
                WardClass.B1,
                WardClass.B2,
                WardClass.C
        );
    }

    @Test
    @DisplayName("Verify Domain Entities getters and setters")
    void testDomainEntities() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Patient patient = new Patient(id, "S****123A", "Tan Ah Meng", 68, Gender.MALE, InfectionStatus.NON_INFECTIOUS, 65, true, "TOKEN-P101");
        assertThat(patient.getId()).isEqualTo(id);
        assertThat(patient.getName()).isEqualTo("Tan Ah Meng");
        assertThat(patient.getNricMasked()).isEqualTo("S****123A");
        assertThat(patient.getAge()).isEqualTo(68);
        assertThat(patient.getGender()).isEqualTo(Gender.MALE);
        assertThat(patient.getInfectionStatus()).isEqualTo(InfectionStatus.NON_INFECTIOUS);
        assertThat(patient.getFallRiskScore()).isEqualTo(65);
        assertThat(patient.isNeedsTelemetry()).isTrue();
        assertThat(patient.getQueueToken()).isEqualTo("TOKEN-P101");

        Ward ward = Ward.builder()
                .id(id)
                .name("Ward 8A")
                .level(8)
                .wardClass(WardClass.B2)
                .serviceCluster(SpecialtyCluster.CARDIOLOGY)
                .lockedGender(Gender.MALE)
                .capacity(6)
                .beds(List.of())
                .build();
        assertThat(ward.getName()).isEqualTo("Ward 8A");
        assertThat(ward.getLevel()).isEqualTo(8);
        assertThat(ward.getWardClass()).isEqualTo(WardClass.B2);
        assertThat(ward.getServiceCluster()).isEqualTo(SpecialtyCluster.CARDIOLOGY);
        assertThat(ward.getLockedGender()).isEqualTo(Gender.MALE);
        assertThat(ward.getCapacity()).isEqualTo(6);
        assertThat(ward.getBeds()).isEmpty();

        Bed bed = Bed.builder()
                .id(id)
                .bedNumber("8A-01")
                .ward(ward)
                .status(BedStatus.EMPTY_CLEANED)
                .hasTelemetry(true)
                .isNearNursingStation(true)
                .currentPatient(patient)
                .lastCleanedAt(now)
                .build();
        assertThat(bed.getId()).isEqualTo(id);
        assertThat(bed.getBedNumber()).isEqualTo("8A-01");
        assertThat(bed.getWard()).isEqualTo(ward);
        assertThat(bed.getStatus()).isEqualTo(BedStatus.EMPTY_CLEANED);
        assertThat(bed.isHasTelemetry()).isTrue();
        assertThat(bed.isNearNursingStation()).isTrue();
        assertThat(bed.getCurrentPatient()).isEqualTo(patient);
        assertThat(bed.getLastCleanedAt()).isEqualTo(now);

        AdmissionRequest req = AdmissionRequest.builder()
                .id(id)
                .patient(patient)
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .secondaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedWardClass(WardClass.B2)
                .status(AdmissionStatus.BED_REQUESTED)
                .assignedBed(bed)
                .requestedAt(now)
                .allocatedAt(now)
                .admittedAt(now)
                .dischargedAt(now)
                .diversionRecommended(true)
                .sisterHospitalReferralId("REF-1")
                .build();
        assertThat(req.getId()).isEqualTo(id);
        assertThat(req.getPatient()).isEqualTo(patient);
        assertThat(req.getSuspectedDiagnosisService()).isEqualTo(SpecialtyCluster.CARDIOLOGY);
        assertThat(req.getPrimaryAcuityTier()).isEqualTo(AcuityTier.TIER_2_ACUTE_URGENT);
        assertThat(req.getSecondaryAcuityTier()).isEqualTo(AcuityTier.TIER_3_ACUTE_STABLE);
        assertThat(req.getRequestedWardClass()).isEqualTo(WardClass.B2);
        assertThat(req.getStatus()).isEqualTo(AdmissionStatus.BED_REQUESTED);
        assertThat(req.getAssignedBed()).isEqualTo(bed);
        assertThat(req.isDiversionRecommended()).isTrue();
        assertThat(req.getSisterHospitalReferralId()).isEqualTo("REF-1");

        AssessmentBroadcast broadcast = AssessmentBroadcast.builder()
                .id(id)
                .admissionRequest(req)
                .targetCluster(SpecialtyCluster.CARDIOLOGY)
                .status(BroadcastStatus.OPEN)
                .claimedBySpecialistId("dr_cardio")
                .claimedAt(now)
                .consultNotes("Notes")
                .build();
        assertThat(broadcast.getId()).isEqualTo(id);
        assertThat(broadcast.getAdmissionRequest()).isEqualTo(req);
        assertThat(broadcast.getTargetCluster()).isEqualTo(SpecialtyCluster.CARDIOLOGY);
        assertThat(broadcast.getStatus()).isEqualTo(BroadcastStatus.OPEN);
        assertThat(broadcast.getClaimedBySpecialistId()).isEqualTo("dr_cardio");
        assertThat(broadcast.getClaimedAt()).isEqualTo(now);
        assertThat(broadcast.getConsultNotes()).isEqualTo("Notes");

        BmuAlgorithmConfig config = new BmuAlgorithmConfig(id, 40, 30, 15, 3);
        assertThat(config.getId()).isEqualTo(id);
        assertThat(config.getWeightSpecialtyCluster()).isEqualTo(40);
        assertThat(config.getWeightConsolidation()).isEqualTo(30);
        assertThat(config.getWeightFallRiskStation()).isEqualTo(15);
        assertThat(config.getBatchHoldingWardThreshold()).isEqualTo(3);

        // Verify AuditableEntity fields
        patient.setCreatedBy("dr_tan_ed");
        patient.setCreatedAt(now);
        patient.setLastModifiedBy("bmu_coord_wong");
        patient.setLastModifiedAt(now);
        assertThat(patient.getCreatedBy()).isEqualTo("dr_tan_ed");
        assertThat(patient.getCreatedAt()).isEqualTo(now);
        assertThat(patient.getLastModifiedBy()).isEqualTo("bmu_coord_wong");
        assertThat(patient.getLastModifiedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Verify DTOs getters, setters, and builders")
    void testDtos() {
        UUID id = UUID.randomUUID();

        BedAllocationRequest allocReq = new BedAllocationRequest(id, id);
        assertThat(allocReq.getAdmissionRequestId()).isEqualTo(id);
        assertThat(allocReq.getBedId()).isEqualTo(id);

        BedRecommendation rec = new BedRecommendation(id, "8A-01", 8, "Ward 8A", 85, List.of("Rule 1"), true);
        assertThat(rec.getBedId()).isEqualTo(id);
        assertThat(rec.getBedNumber()).isEqualTo("8A-01");
        assertThat(rec.getLevel()).isEqualTo(8);
        assertThat(rec.getWardName()).isEqualTo("Ward 8A");
        assertThat(rec.getScore()).isEqualTo(85);
        assertThat(rec.getScoreBreakdown()).contains("Rule 1");
        assertThat(rec.isRecommended()).isTrue();

        BmuConfigUpdateRequest configReq = new BmuConfigUpdateRequest(45, 25, 20, 4);
        assertThat(configReq.getWeightSpecialtyCluster()).isEqualTo(45);
        assertThat(configReq.getWeightConsolidation()).isEqualTo(25);
        assertThat(configReq.getWeightFallRiskStation()).isEqualTo(20);
        assertThat(configReq.getBatchHoldingWardThreshold()).isEqualTo(4);

        DiversionReferralRequest divReq = new DiversionReferralRequest(id, "OCH");
        assertThat(divReq.getAdmissionRequestId()).isEqualTo(id);
        assertThat(divReq.getFacility()).isEqualTo("OCH");

        EdAssessmentSubmitRequest edReq = new EdAssessmentSubmitRequest(id, SpecialtyCluster.CARDIOLOGY, AcuityTier.TIER_2_ACUTE_URGENT, WardClass.B2, true);
        assertThat(edReq.getPatientId()).isEqualTo(id);
        assertThat(edReq.getSuspectedDiagnosisService()).isEqualTo(SpecialtyCluster.CARDIOLOGY);
        assertThat(edReq.getPrimaryAcuityTier()).isEqualTo(AcuityTier.TIER_2_ACUTE_URGENT);
        assertThat(edReq.getRequestedWardClass()).isEqualTo(WardClass.B2);
        assertThat(edReq.isNeedsTelemetry()).isTrue();

        PatientEhrSummary ehr = new PatientEhrSummary("S****123A", "None", "None", "Normal", "Clear");
        assertThat(ehr.getNricMasked()).isEqualTo("S****123A");

        PatientMilestoneResponse milestone = PatientMilestoneResponse.builder()
                .patientId(id)
                .patientName("John")
                .queueToken("TOKEN-1")
                .admissionStatus(AdmissionStatus.BED_REQUESTED)
                .queuePosition(1)
                .estimatedWaitMinutes(25)
                .assignedBedNumber("8A-01")
                .assignedWardName("Ward 8A")
                .assignedLevel(8)
                .coPayEstimate("$30")
                .careGuidance("Wait seated")
                .build();
        assertThat(milestone.getPatientId()).isEqualTo(id);
        assertThat(milestone.getPatientName()).isEqualTo("John");
        assertThat(milestone.getQueueToken()).isEqualTo("TOKEN-1");
        assertThat(milestone.getAdmissionStatus()).isEqualTo(AdmissionStatus.BED_REQUESTED);

        SisterHospitalReferralResponse sisRes = new SisterHospitalReferralResponse("REF-1", "OCH", "ACCEPTED", 30, "Notes");
        assertThat(sisRes.getReferralId()).isEqualTo("REF-1");
        assertThat(sisRes.getDestinationFacility()).isEqualTo("OCH");
        assertThat(sisRes.getStatus()).isEqualTo("ACCEPTED");
        assertThat(sisRes.getSlaWindowMinutes()).isEqualTo(30);
        assertThat(sisRes.getNotes()).isEqualTo("Notes");

        SpecialistConsultRequest specReq = new SpecialistConsultRequest(AcuityTier.TIER_2_ACUTE_URGENT, "Consult notes", false);
        assertThat(specReq.getConsultNotes()).isEqualTo("Consult notes");
        assertThat(specReq.getSecondaryAcuityTier()).isEqualTo(AcuityTier.TIER_2_ACUTE_URGENT);
        assertThat(specReq.isDiversionRecommended()).isFalse();
    }
}
