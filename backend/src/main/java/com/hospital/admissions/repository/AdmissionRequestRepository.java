package com.hospital.admissions.repository;

import com.hospital.admissions.entity.AdmissionRequest;
import com.hospital.admissions.entity.AdmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AdmissionRequest} entity operations.
 */
@Repository
public interface AdmissionRequestRepository extends JpaRepository<AdmissionRequest, UUID> {

    /**
     * Finds admission requests with the specified admission lifecycle status.
     *
     * @param status the {@link AdmissionStatus} to filter by.
     * @return list of matching {@link AdmissionRequest} entities.
     */
    List<AdmissionRequest> findByStatus(AdmissionStatus status);

    /**
     * Finds an admission request associated with a patient by their unique queue token.
     *
     * @param queueToken the alphanumeric patient token.
     * @return optional containing the matching {@link AdmissionRequest}, if found.
     */
    Optional<AdmissionRequest> findByPatient_QueueToken(String queueToken);

    /**
     * Finds an admission request associated with a specific patient identifier.
     *
     * @param patientId the unique identifier of the patient.
     * @return optional containing the matching {@link AdmissionRequest}, if found.
     */
    Optional<AdmissionRequest> findByPatient_Id(UUID patientId);

    /**
     * Finds the admission request currently allocated to a specific bed identifier.
     *
     * @param bedId the unique identifier of the bed.
     * @return optional containing the matching {@link AdmissionRequest}, if found.
     */
    Optional<AdmissionRequest> findByAssignedBed_Id(UUID bedId);

    /**
     * Retrieves all admission requests ordered by their requested timestamp in descending order.
     *
     * @return ordered list of {@link AdmissionRequest} instances.
     */
    List<AdmissionRequest> findAllByOrderByRequestedAtDesc();
}
