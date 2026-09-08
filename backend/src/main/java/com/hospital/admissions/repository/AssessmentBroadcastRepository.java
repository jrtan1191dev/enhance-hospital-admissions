package com.hospital.admissions.repository;

import com.hospital.admissions.domain.AssessmentBroadcast;
import com.hospital.admissions.domain.BroadcastStatus;
import com.hospital.admissions.domain.SpecialtyCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssessmentBroadcastRepository extends JpaRepository<AssessmentBroadcast, UUID> {
    List<AssessmentBroadcast> findByTargetCluster(SpecialtyCluster targetCluster);
    List<AssessmentBroadcast> findByStatus(BroadcastStatus status);
    List<AssessmentBroadcast> findByAdmissionRequest_Id(UUID admissionRequestId);
}
