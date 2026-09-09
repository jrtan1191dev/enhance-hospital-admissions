package com.hospital.admissions.repository;

import com.hospital.admissions.domain.AdmissionRequest;
import com.hospital.admissions.domain.AdmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdmissionRequestRepository extends JpaRepository<AdmissionRequest, UUID> {
    List<AdmissionRequest> findByStatus(AdmissionStatus status);
    Optional<AdmissionRequest> findByPatient_QueueToken(String queueToken);
    Optional<AdmissionRequest> findByPatient_Id(UUID patientId);
    Optional<AdmissionRequest> findByAssignedBed_Id(UUID bedId);
    List<AdmissionRequest> findAllByOrderByRequestedAtDesc();
}
