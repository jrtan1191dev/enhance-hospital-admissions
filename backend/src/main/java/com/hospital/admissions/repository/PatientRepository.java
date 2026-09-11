package com.hospital.admissions.repository;

import com.hospital.admissions.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Patient} demographic and clinical profiles.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    /**
     * Finds a patient record by their public tracking token.
     *
     * @param queueToken the alphanumeric tracking token.
     * @return optional containing the matching {@link Patient}.
     */
    Optional<Patient> findByQueueToken(String queueToken);

    /**
     * Queries patients who are currently waiting in the emergency intake area without an active admission.
     *
     * @return list of {@link Patient} entities without active admissions.
     */
    @Query("SELECT p FROM Patient p WHERE p.id NOT IN " +
            "(SELECT ar.patient.id FROM AdmissionRequest ar WHERE ar.status != com.hospital.admissions.entity.AdmissionStatus.DISCHARGED)")
    List<Patient> findPatientsWithoutActiveAdmission();
}
