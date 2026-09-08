package com.hospital.admissions.web;

import com.hospital.admissions.domain.Bed;
import com.hospital.admissions.domain.BedStatus;
import com.hospital.admissions.domain.Patient;
import com.hospital.admissions.dto.PatientMilestoneResponse;
import com.hospital.admissions.repository.PatientRepository;
import com.hospital.admissions.service.PatientTrackerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PatientTrackerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PatientTrackerService patientTrackerService;

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientTrackerController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/patients/track/{token} returns milestone response")
    void testTrackPatient() throws Exception {
        PatientMilestoneResponse response = PatientMilestoneResponse.builder()
                .queueToken("TOKEN-123")
                .patientName("Alice")
                .build();

        when(patientTrackerService.trackPatient("TOKEN-123")).thenReturn(response);

        mockMvc.perform(get("/api/v1/patients/track/TOKEN-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueToken").value("TOKEN-123"))
                .andExpect(jsonPath("$.patientName").value("Alice"));
    }

    @Test
    @DisplayName("GET /api/v1/patients/tokens returns patient list")
    void testGetAvailablePatientsForPicker() throws Exception {
        Patient patient = Patient.builder().name("Bob").build();
        when(patientRepository.findAll()).thenReturn(List.of(patient));

        mockMvc.perform(get("/api/v1/patients/tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bob"));
    }

    @Test
    @DisplayName("POST /api/v1/patients/beds/{bedId}/checkin returns checked in bed")
    void testCheckinPatient() throws Exception {
        UUID bedId = UUID.randomUUID();
        Bed bed = Bed.builder().id(bedId).status(BedStatus.OCCUPIED_TAKEN).build();
        when(patientTrackerService.checkinPatient(bedId)).thenReturn(bed);

        mockMvc.perform(post("/api/v1/patients/beds/" + bedId + "/checkin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OCCUPIED_TAKEN"));
    }

    @Test
    @DisplayName("POST /api/v1/patients/beds/{bedId}/vacate returns vacated bed")
    void testVacatePatient() throws Exception {
        UUID bedId = UUID.randomUUID();
        Bed bed = Bed.builder().id(bedId).status(BedStatus.EMPTY_PENDING_CLEANING).build();
        when(patientTrackerService.vacatePatient(bedId)).thenReturn(bed);

        mockMvc.perform(post("/api/v1/patients/beds/" + bedId + "/vacate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EMPTY_PENDING_CLEANING"));
    }

    @Test
    @DisplayName("POST /api/v1/patients/beds/{bedId}/clean returns cleaned bed")
    void testCleanBed() throws Exception {
        UUID bedId = UUID.randomUUID();
        Bed bed = Bed.builder().id(bedId).status(BedStatus.EMPTY_CLEANED).build();
        when(patientTrackerService.cleanBed(bedId)).thenReturn(bed);

        mockMvc.perform(post("/api/v1/patients/beds/" + bedId + "/clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EMPTY_CLEANED"));
    }
}
