package com.hospital.admissions.repository;

import com.hospital.admissions.domain.PatientAuditInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientAuditInteractionRepository extends JpaRepository<PatientAuditInteraction, UUID> {
    List<PatientAuditInteraction> findByToken(String token);
    List<PatientAuditInteraction> findByAdmissionId(UUID admissionId);
}
