package com.hospital.admissions.solver;

import com.hospital.admissions.domain.*;
import com.hospital.admissions.dto.BedRecommendation;
import com.hospital.admissions.repository.BedRepository;
import com.hospital.admissions.repository.WardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeuristicBedAllocationSolverTest {

    @Mock
    private BedRepository bedRepository;

    @Mock
    private WardRepository wardRepository;

    @InjectMocks
    private HeuristicBedAllocationSolver solver;

    @Test
    @DisplayName("recommendBeds filters by WardClass, Gender, Telemetry, and Infection constraints and scores candidates")
    void testRecommendBeds_ComprehensiveRules() {
        BmuAlgorithmConfig config = BmuAlgorithmConfig.builder()
                .weightSpecialtyCluster(40)
                .weightConsolidation(30)
                .weightFallRiskStation(15)
                .build();

        Patient cardioPatient = Patient.builder()
                .id(UUID.randomUUID())
                .name("Tan Ah Meng")
                .gender(Gender.MALE)
                .needsTelemetry(true)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(65)
                .build();

        AdmissionRequest request = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(cardioPatient)
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .requestedWardClass(WardClass.B2)
                .build();

        UUID wardCardioId = UUID.randomUUID();
        Ward wardCardio = Ward.builder()
                .id(wardCardioId)
                .name("Ward 8A")
                .level(8)
                .wardClass(WardClass.B2)
                .serviceCluster(SpecialtyCluster.CARDIOLOGY)
                .lockedGender(Gender.MALE)
                .capacity(6)
                .build();

        UUID wardSurgeryId = UUID.randomUUID();
        Ward wardSurgery = Ward.builder()
                .id(wardSurgeryId)
                .name("Ward 8B")
                .level(8)
                .wardClass(WardClass.B2)
                .serviceCluster(SpecialtyCluster.SURGERY)
                .lockedGender(Gender.MALE)
                .capacity(6)
                .build();

        UUID wardWrongClassId = UUID.randomUUID();
        Ward wardWrongClass = Ward.builder()
                .id(wardWrongClassId)
                .name("Ward 9A")
                .level(9)
                .wardClass(WardClass.C)
                .serviceCluster(SpecialtyCluster.CARDIOLOGY)
                .capacity(8)
                .build();

        UUID wardFemaleId = UUID.randomUUID();
        Ward wardFemale = Ward.builder()
                .id(wardFemaleId)
                .name("Ward 8C")
                .level(8)
                .wardClass(WardClass.B2)
                .serviceCluster(SpecialtyCluster.CARDIOLOGY)
                .lockedGender(Gender.FEMALE)
                .capacity(6)
                .build();

        // Bed 1: Perfect candidate - Ward 8A (Cardio, B2, Male), has telemetry, near station
        Bed bed1 = Bed.builder()
                .id(UUID.randomUUID())
                .bedNumber("8A-03")
                .ward(wardCardio)
                .status(BedStatus.EMPTY_CLEANED)
                .hasTelemetry(true)
                .isNearNursingStation(true)
                .build();

        // Bed 2: Ward 8B (Surgery != Cardio), has telemetry, not near station
        Bed bed2 = Bed.builder()
                .id(UUID.randomUUID())
                .bedNumber("8B-02")
                .ward(wardSurgery)
                .status(BedStatus.EMPTY_CLEANED)
                .hasTelemetry(true)
                .isNearNursingStation(false)
                .build();

        // Bed 3: Wrong ward class (C != B2) -> Hard constraint eliminates
        Bed bed3 = Bed.builder()
                .id(UUID.randomUUID())
                .bedNumber("9A-01")
                .ward(wardWrongClass)
                .status(BedStatus.EMPTY_CLEANED)
                .hasTelemetry(true)
                .build();

        // Bed 4: Wrong gender lock (Female != Male) -> Hard constraint eliminates
        Bed bed4 = Bed.builder()
                .id(UUID.randomUUID())
                .bedNumber("8C-01")
                .ward(wardFemale)
                .status(BedStatus.EMPTY_CLEANED)
                .hasTelemetry(true)
                .build();

        // Bed 5: In Ward 8A but no telemetry -> Hard constraint eliminates because patient needs telemetry
        Bed bed5 = Bed.builder()
                .id(UUID.randomUUID())
                .bedNumber("8A-04")
                .ward(wardCardio)
                .status(BedStatus.EMPTY_CLEANED)
                .hasTelemetry(false)
                .build();

        when(bedRepository.findByStatus(BedStatus.EMPTY_CLEANED))
                .thenReturn(List.of(bed1, bed2, bed3, bed4, bed5));

        // Ward 8A has occupied bed -> triggers consolidation packing bonus
        Bed occupiedBed = Bed.builder().status(BedStatus.OCCUPIED_TAKEN).build();
        when(bedRepository.findByWard_Id(wardCardioId)).thenReturn(List.of(bed1, occupiedBed));
        when(bedRepository.findByWard_Id(wardSurgeryId)).thenReturn(List.of(bed2));

        List<BedRecommendation> recommendations = solver.recommendBeds(request, config);

        // Expect bed1 and bed2 only
        assertThat(recommendations).hasSize(2);

        // Bed1: +40 (Specialty) + 30 (Consolidation) + 15 (Fall Risk 65 >= 45 & Near Station) = 85
        BedRecommendation topRec = recommendations.get(0);
        assertThat(topRec.getBedNumber()).isEqualTo("8A-03");
        assertThat(topRec.getScore()).isEqualTo(85);
        assertThat(topRec.isRecommended()).isTrue();
        assertThat(topRec.getScoreBreakdown()).contains(
                "+40 Specialty (CARDIOLOGY)",
                "+30 Consolidation (Preserves Flex Wards)",
                "+15 Proximity (Fall Risk 65 >= 45)"
        );

        // Bed2: 0 (No specialty match, no consolidation in 8B, not near station) = 0
        BedRecommendation secondRec = recommendations.get(1);
        assertThat(secondRec.getBedNumber()).isEqualTo("8B-02");
        assertThat(secondRec.getScore()).isEqualTo(0);
        assertThat(secondRec.isRecommended()).isTrue();
    }

    @Test
    @DisplayName("recommendBeds eliminates multi-bed wards with occupants for infectious patients")
    void testRecommendBeds_InfectionControl() {
        Patient mrsaPatient = Patient.builder()
                .id(UUID.randomUUID())
                .gender(Gender.FEMALE)
                .needsTelemetry(false)
                .infectionStatus(InfectionStatus.MRSA)
                .fallRiskScore(20)
                .build();

        AdmissionRequest request = AdmissionRequest.builder()
                .patient(mrsaPatient)
                .suspectedDiagnosisService(SpecialtyCluster.GENERAL_MEDICINE)
                .requestedWardClass(WardClass.B2)
                .build();

        UUID wardId = UUID.randomUUID();
        Ward multiBedWard = Ward.builder()
                .id(wardId)
                .name("Ward 9B")
                .wardClass(WardClass.B2)
                .serviceCluster(SpecialtyCluster.GENERAL_MEDICINE)
                .capacity(6)
                .build();

        Bed cleanBed = Bed.builder()
                .id(UUID.randomUUID())
                .bedNumber("9B-01")
                .ward(multiBedWard)
                .status(BedStatus.EMPTY_CLEANED)
                .build();

        Bed occupiedBed = Bed.builder()
                .status(BedStatus.OCCUPIED_TAKEN)
                .build();

        when(bedRepository.findByStatus(BedStatus.EMPTY_CLEANED)).thenReturn(List.of(cleanBed));
        when(bedRepository.findByWard_Id(wardId)).thenReturn(List.of(cleanBed, occupiedBed));

        List<BedRecommendation> recommendations = solver.recommendBeds(request, null);

        // Should be eliminated due to Hard Constraint 4 (Infection control in occupied multi-bed ward)
        assertThat(recommendations).isEmpty();
    }
}
