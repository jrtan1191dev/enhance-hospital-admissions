package com.hospital.admissions.service;

import com.hospital.admissions.domain.*;
import com.hospital.admissions.dto.PatientMilestoneResponse;
import com.hospital.admissions.repository.AdmissionRequestRepository;
import com.hospital.admissions.repository.BedRepository;
import com.hospital.admissions.repository.PatientRepository;
import com.hospital.admissions.security.AuditLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientTrackerServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AdmissionRequestRepository admissionRequestRepository;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private PatientTrackerService trackerService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("nurse_test", null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("trackPatient throws when token is not found")
    void testTrackPatient_TokenNotFound() {
        when(patientRepository.findByQueueToken("TOKEN-NONE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackerService.trackPatient("TOKEN-NONE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Patient not found for token");
    }

    @Test
    @DisplayName("trackPatient without AdmissionRequest returns ASSESSMENT_PENDING status")
    void testTrackPatient_NoAdmissionRequest() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("Alice").queueToken("TOKEN-A").build();

        when(patientRepository.findByQueueToken("TOKEN-A")).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.findByPatient_QueueToken("TOKEN-A")).thenReturn(Optional.empty());

        PatientMilestoneResponse res = trackerService.trackPatient("TOKEN-A");

        assertThat(res.getAdmissionStatus()).isEqualTo(AdmissionStatus.ASSESSMENT_PENDING);
        assertThat(res.getQueuePosition()).isEqualTo(1);
        assertThat(res.getEstimatedWaitMinutes()).isEqualTo(15);
        assertThat(res.getAssignedBedNumber()).isNull();
    }

    @Test
    @DisplayName("trackPatient with BED_REQUESTED calculates queue position and wait minutes")
    void testTrackPatient_BedRequested() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("Alice").queueToken("TOKEN-A").build();

        UUID reqId = UUID.randomUUID();
        AdmissionRequest myReq = AdmissionRequest.builder()
                .id(reqId)
                .patient(patient)
                .status(AdmissionStatus.BED_REQUESTED)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B2)
                .requestedAt(LocalDateTime.now())
                .build();

        AdmissionRequest otherReq = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .primaryAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .requestedAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(patientRepository.findByQueueToken("TOKEN-A")).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.findByPatient_QueueToken("TOKEN-A")).thenReturn(Optional.of(myReq));
        when(admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED))
                .thenReturn(List.of(otherReq, myReq));

        PatientMilestoneResponse res = trackerService.trackPatient("TOKEN-A");

        assertThat(res.getAdmissionStatus()).isEqualTo(AdmissionStatus.BED_REQUESTED);
        assertThat(res.getQueuePosition()).isEqualTo(2);
        assertThat(res.getEstimatedWaitMinutes()).isEqualTo(50); // position 2 * 25
    }

    @Test
    @DisplayName("trackPatient with BED_ALLOCATED returns wait 5m and bed location details")
    void testTrackPatient_BedAllocated() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("Bob").queueToken("TOKEN-B").build();

        Ward ward = Ward.builder().name("Ward 8A").level(8).build();
        Bed bed = Bed.builder().bedNumber("8A-02").ward(ward).build();

        AdmissionRequest req = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .status(AdmissionStatus.BED_ALLOCATED)
                .requestedWardClass(WardClass.B2)
                .assignedBed(bed)
                .build();

        when(patientRepository.findByQueueToken("TOKEN-B")).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.findByPatient_QueueToken("TOKEN-B")).thenReturn(Optional.of(req));

        PatientMilestoneResponse res = trackerService.trackPatient("TOKEN-B");

        assertThat(res.getAdmissionStatus()).isEqualTo(AdmissionStatus.BED_ALLOCATED);
        assertThat(res.getQueuePosition()).isEqualTo(0);
        assertThat(res.getEstimatedWaitMinutes()).isEqualTo(5);
        assertThat(res.getAssignedBedNumber()).isEqualTo("8A-02");
        assertThat(res.getAssignedWardName()).isEqualTo("Ward 8A");
        assertThat(res.getAssignedLevel()).isEqualTo(8);
    }

    @Test
    @DisplayName("trackPatient with ADMITTED_INPATIENT or DISCHARGED returns 0 wait minutes")
    void testTrackPatient_InpatientOrDischarged() {
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).name("Charlie").queueToken("TOKEN-C").build();

        AdmissionRequest req = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .status(AdmissionStatus.ADMITTED_INPATIENT)
                .requestedWardClass(WardClass.B2)
                .build();

        when(patientRepository.findByQueueToken("TOKEN-C")).thenReturn(Optional.of(patient));
        when(admissionRequestRepository.findByPatient_QueueToken("TOKEN-C")).thenReturn(Optional.of(req));

        PatientMilestoneResponse res = trackerService.trackPatient("TOKEN-C");

        assertThat(res.getAdmissionStatus()).isEqualTo(AdmissionStatus.ADMITTED_INPATIENT);
        assertThat(res.getQueuePosition()).isEqualTo(0);
        assertThat(res.getEstimatedWaitMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("checkinPatient transitions bed to OCCUPIED_TAKEN and updates AdmissionRequest")
    void testCheckinPatient_Success() {
        UUID bedId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).build();
        Bed bed = Bed.builder().id(bedId).bedNumber("8A-01").currentPatient(patient).build();
        AdmissionRequest req = AdmissionRequest.builder().id(UUID.randomUUID()).build();

        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(bedRepository.save(bed)).thenReturn(bed);
        when(admissionRequestRepository.findByPatient_Id(patientId)).thenReturn(Optional.of(req));

        Bed result = trackerService.checkinPatient(bedId);

        assertThat(result.getStatus()).isEqualTo(BedStatus.OCCUPIED_TAKEN);
        assertThat(req.getStatus()).isEqualTo(AdmissionStatus.ADMITTED_INPATIENT);
        assertThat(req.getAdmittedAt()).isNotNull();
        verify(admissionRequestRepository).save(req);
        verify(auditLogger).logAction(eq("nurse_test"), eq("CHECKIN_PATIENT"), anyString(), anyString());
    }

    @Test
    @DisplayName("checkinPatient throws when bed is not found")
    void testCheckinPatient_NotFound() {
        UUID bedId = UUID.randomUUID();
        when(bedRepository.findById(bedId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackerService.checkinPatient(bedId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bed not found");
    }

    @Test
    @DisplayName("vacatePatient transitions bed to EMPTY_PENDING_CLEANING and discharges request")
    void testVacatePatient_Success() {
        UUID bedId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Patient patient = Patient.builder().id(patientId).build();
        Bed bed = Bed.builder().id(bedId).bedNumber("8A-01").currentPatient(patient).build();
        AdmissionRequest req = AdmissionRequest.builder().id(UUID.randomUUID()).build();

        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(bedRepository.save(bed)).thenReturn(bed);
        when(admissionRequestRepository.findByPatient_Id(patientId)).thenReturn(Optional.of(req));

        Bed result = trackerService.vacatePatient(bedId);

        assertThat(result.getStatus()).isEqualTo(BedStatus.EMPTY_PENDING_CLEANING);
        assertThat(req.getStatus()).isEqualTo(AdmissionStatus.DISCHARGED);
        assertThat(req.getDischargedAt()).isNotNull();
        verify(admissionRequestRepository).save(req);
        verify(auditLogger).logAction(eq("nurse_test"), eq("VACATE_PATIENT"), anyString(), anyString());
    }

    @Test
    @DisplayName("vacatePatient throws when bed is not found")
    void testVacatePatient_NotFound() {
        UUID bedId = UUID.randomUUID();
        when(bedRepository.findById(bedId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackerService.vacatePatient(bedId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bed not found");
    }

    @Test
    @DisplayName("cleanBed transitions bed to EMPTY_CLEANED and clears current patient")
    void testCleanBed_Success() {
        UUID bedId = UUID.randomUUID();
        Patient patient = Patient.builder().id(UUID.randomUUID()).build();
        Bed bed = Bed.builder()
                .id(bedId)
                .bedNumber("8A-01")
                .status(BedStatus.EMPTY_PENDING_CLEANING)
                .currentPatient(patient)
                .build();

        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(bedRepository.save(bed)).thenReturn(bed);

        Bed result = trackerService.cleanBed(bedId);

        assertThat(result.getStatus()).isEqualTo(BedStatus.EMPTY_CLEANED);
        assertThat(result.getLastCleanedAt()).isNotNull();
        assertThat(result.getCurrentPatient()).isNull();
        verify(auditLogger).logAction(eq("nurse_test"), eq("CLEAN_BED"), anyString(), anyString());
    }

    @Test
    @DisplayName("cleanBed throws when bed is not found")
    void testCleanBed_NotFound() {
        UUID bedId = UUID.randomUUID();
        when(bedRepository.findById(bedId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackerService.cleanBed(bedId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bed not found");
    }
}
