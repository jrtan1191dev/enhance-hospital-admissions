package com.hospital.admissions.repository;

import com.hospital.admissions.entity.PatientAuditInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for logging and querying {@link PatientAuditInteraction} events.
 */
@Repository
public interface PatientAuditInteractionRepository extends JpaRepository<PatientAuditInteraction, UUID> {

    /**
     * Finds audit interactions matching a patient tracking token.
     *
     * @param token the patient tracking token.
     * @return list of matching {@link PatientAuditInteraction} records.
     */
    List<PatientAuditInteraction> findByToken(String token);

    /**
     * Finds audit interactions associated with an admission request ID.
     *
     * @param admissionId the unique identifier of the admission request.
     * @return list of matching {@link PatientAuditInteraction} records.
     */
    List<PatientAuditInteraction> findByAdmissionId(UUID admissionId);
}
