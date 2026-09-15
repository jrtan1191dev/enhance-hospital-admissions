package com.hospital.admissions.repository.spec;

import com.hospital.admissions.entity.AdmissionRequest;
import com.hospital.admissions.entity.Patient;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Reusable {@link Specification} definitions for querying {@link Patient} entities.
 * <p>
 * Centralising criteria here keeps dynamic query logic out of the repository interface and lets
 * callers compose predicates with {@link Specification#and}/{@link Specification#or}.
 */
public final class PatientSpecifications {

    private PatientSpecifications() {
    }

    /**
     * Matches patients waiting in the emergency intake area who have not yet entered the admission
     * pipeline — i.e. those with no admission request of any status.
     * <p>
     * A patient with any admission request (including a terminal {@code DISCHARGED} one) has already
     * progressed past ED intake and must not appear on the awaiting-assessment board. Equivalent to
     * the JPQL {@code p.id NOT IN (SELECT ar.patient.id FROM AdmissionRequest ar)}.
     *
     * @return specification selecting patients with no associated admission request.
     */
    public static Specification<Patient> withoutActiveAdmission() {
        return (root, query, cb) -> {
            Subquery<UUID> admittedPatientIds = query.subquery(UUID.class);
            Root<AdmissionRequest> admission = admittedPatientIds.from(AdmissionRequest.class);
            admittedPatientIds.select(admission.get("patient").get("id"));
            return cb.not(root.get("id").in(admittedPatientIds));
        };
    }
}
