package com.hospital.admissions.service;

import com.hospital.admissions.entity.*;
import com.hospital.admissions.dto.BmuCapacityForecastDto;
import com.hospital.admissions.dto.DischargeRunwayDto;
import com.hospital.admissions.dto.EddUpdateRequest;
import com.hospital.admissions.repository.AdmissionRequestRepository;
import com.hospital.admissions.repository.BedRepository;
import com.hospital.admissions.security.AuditLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WardServiceTest {

    @Mock
    private AdmissionRequestRepository admissionRequestRepository;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private AuditLogger auditLogger;

    private WardService wardService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dr_chen", null, List.of())
        );
        wardService = new WardService(admissionRequestRepository, bedRepository, auditLogger);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("updateEdd updates EDD, confidence, rationale and logs RECORD_EDD audit event")
    void testUpdateEdd_Success() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("Uncle Seng").build();
        AdmissionRequest admissionRequest = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .status(AdmissionStatus.ADMITTED_INPATIENT)
                .build();

        LocalDate targetEdd = LocalDate.now().plusDays(2);
        EddUpdateRequest request = EddUpdateRequest.builder()
                .edd(targetEdd)
                .eddConfidence(EddConfidence.HIGH)
                .rationale("Post-op recovery progressing well, stable vitals")
                .build();

        when(admissionRequestRepository.findByPatient_Id(patientId)).thenReturn(Optional.of(admissionRequest));
        when(admissionRequestRepository.save(any(AdmissionRequest.class))).thenAnswer(i -> i.getArgument(0));

        AdmissionRequest result = wardService.updateEdd(patientId, request);

        assertThat(result.getEdd()).isEqualTo(targetEdd);
        assertThat(result.getEddConfidence()).isEqualTo(EddConfidence.HIGH);
        assertThat(result.getEddRationale()).isEqualTo("Post-op recovery progressing well, stable vitals");

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogger).logAction(eq("dr_chen"), eq("RECORD_EDD"), eq("AdmissionRequest:" + admissionRequest.getId()), detailsCaptor.capture());

        String details = detailsCaptor.getValue();
        assertThat(details).contains("PatientId=" + patientId)
                .contains("EDD=" + targetEdd)
                .contains("Confidence=HIGH")
                .contains("RunwayStage=RUNWAY_D2");
    }

    @Test
    @DisplayName("updateEdd throws IllegalArgumentException when admission request not found")
    void testUpdateEdd_NotFound() {
        UUID patientId = UUID.randomUUID();
        EddUpdateRequest request = EddUpdateRequest.builder()
                .edd(LocalDate.now().plusDays(1))
                .eddConfidence(EddConfidence.MEDIUM)
                .build();

        when(admissionRequestRepository.findByPatient_Id(patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wardService.updateEdd(patientId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Admission request not found");
    }

    @Test
    @DisplayName("calculateRunwayStage maps dates correctly to stages")
    void testCalculateRunwayStage() {
        LocalDate today = LocalDate.now();

        assertThat(wardService.calculateRunwayStage(today.plusDays(3), null, null)).isEqualTo(DischargeRunwayStage.RUNWAY_D3);
        assertThat(wardService.calculateRunwayStage(today.plusDays(2), null, null)).isEqualTo(DischargeRunwayStage.RUNWAY_D2);
        assertThat(wardService.calculateRunwayStage(today.plusDays(1), null, null)).isEqualTo(DischargeRunwayStage.RUNWAY_D1);
        assertThat(wardService.calculateRunwayStage(today, null, null)).isEqualTo(DischargeRunwayStage.READY_FOR_MORNING_SIGNOFF);
        assertThat(wardService.calculateRunwayStage(today, LocalDateTime.now(), MedicationDeliveryStatus.PACKING_IN_PROGRESS)).isEqualTo(DischargeRunwayStage.MEDICATIONS_PENDING);
        assertThat(wardService.calculateRunwayStage(today, LocalDateTime.now(), MedicationDeliveryStatus.DELIVERED_BEDSIDE)).isEqualTo(DischargeRunwayStage.READY_TO_VACATE);
    }

    @Test
    @DisplayName("getRunway returns active inpatient runway entries")
    void testGetRunway() {
        UUID pId = UUID.randomUUID();
        Patient p = Patient.builder().id(pId).name("Uncle Seng").build();
        Ward ward = Ward.builder().name("Ward 8A").level(8).serviceCluster(SpecialtyCluster.CARDIOLOGY).build();
        Bed bed = Bed.builder().id(UUID.randomUUID()).bedNumber("8A-01").ward(ward).build();

        AdmissionRequest req = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(p)
                .assignedBed(bed)
                .status(AdmissionStatus.ADMITTED_INPATIENT)
                .edd(LocalDate.now().plusDays(1))
                .eddConfidence(EddConfidence.HIGH)
                .eddRationale("Discharge tomorrow morning")
                .medicationDeliveryStatus(MedicationDeliveryStatus.NOT_DISPATCHED)
                .build();

        when(admissionRequestRepository.findByStatus(AdmissionStatus.ADMITTED_INPATIENT)).thenReturn(List.of(req));

        List<DischargeRunwayDto> runway = wardService.getRunway();

        assertThat(runway).hasSize(1);
        DischargeRunwayDto dto = runway.get(0);
        assertThat(dto.getPatientName()).isEqualTo("Uncle Seng");
        assertThat(dto.getBedNumber()).isEqualTo("8A-01");
        assertThat(dto.getWardCode()).isEqualTo("Ward 8A");
        assertThat(dto.getLevelNumber()).isEqualTo(8);
        assertThat(dto.getRunwayStage()).isEqualTo(DischargeRunwayStage.RUNWAY_D1);
        assertThat(dto.getConfidence()).isEqualTo(EddConfidence.HIGH);
    }

    @Test
    @DisplayName("getCapacityForecast computes 24, 48, 72h projections by ward and cluster")
    void testGetCapacityForecast() {
        LocalDate today = LocalDate.now();
        Ward ward8A = Ward.builder().name("Ward 8A").serviceCluster(SpecialtyCluster.CARDIOLOGY).build();
        Bed bed8A = Bed.builder().bedNumber("8A-01").ward(ward8A).build();

        AdmissionRequest req24 = AdmissionRequest.builder()
                .assignedBed(bed8A)
                .status(AdmissionStatus.ADMITTED_INPATIENT)
                .edd(today.plusDays(1))
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .build();

        AdmissionRequest req48 = AdmissionRequest.builder()
                .assignedBed(bed8A)
                .status(AdmissionStatus.ADMITTED_INPATIENT)
                .edd(today.plusDays(2))
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .build();

        when(admissionRequestRepository.findByStatus(AdmissionStatus.ADMITTED_INPATIENT)).thenReturn(List.of(req24, req48));

        BmuCapacityForecastDto forecast = wardService.getCapacityForecast();

        assertThat(forecast.getTotalNext24Hours()).isEqualTo(1);
        assertThat(forecast.getTotalNext48Hours()).isEqualTo(1);
        assertThat(forecast.getTotalNext72Hours()).isEqualTo(0);
        assertThat(forecast.getByWard()).isNotEmpty();
        assertThat(forecast.getByCluster()).isNotEmpty();
    }
}
