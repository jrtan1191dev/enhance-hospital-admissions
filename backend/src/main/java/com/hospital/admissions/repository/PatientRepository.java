package com.hospital.admissions.repository;

import com.hospital.admissions.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Patient} demographic and clinical profiles.
 * <p>
 * Extends {@link JpaSpecificationExecutor} so callers can compose dynamic query criteria via
 * {@link com.hospital.admissions.repository.spec.PatientSpecifications} instead of hardcoded JPQL.
 */
@Repository
public interface PatientRepository
        extends JpaRepository<Patient, UUID>, JpaSpecificationExecutor<Patient> {

    /**
     * Finds a patient record by their public tracking token.
     *
     * @param queueToken the alphanumeric tracking token.
     * @return optional containing the matching {@link Patient}.
     */
    Optional<Patient> findByQueueToken(String queueToken);
}
