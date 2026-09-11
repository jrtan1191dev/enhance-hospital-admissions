package com.hospital.admissions.repository;

import com.hospital.admissions.entity.AssessmentBroadcast;
import com.hospital.admissions.entity.BroadcastStatus;
import com.hospital.admissions.entity.SpecialtyCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AssessmentBroadcast} entity operations.
 */
@Repository
public interface AssessmentBroadcastRepository extends JpaRepository<AssessmentBroadcast, UUID> {

    /**
     * Finds broadcasts targeting a specific clinical specialty cluster.
     *
     * @param targetCluster the {@link SpecialtyCluster} to filter by.
     * @return list of matching {@link AssessmentBroadcast} objects.
     */
    List<AssessmentBroadcast> findByTargetCluster(SpecialtyCluster targetCluster);

    /**
     * Finds broadcasts matching a given lifecycle status (OPEN, CLAIMED, AUTO_ESCALATED, COMPLETED).
     *
     * @param status the {@link BroadcastStatus} to filter by.
     * @return list of matching {@link AssessmentBroadcast} objects.
     */
    List<AssessmentBroadcast> findByStatus(BroadcastStatus status);

    /**
     * Finds all consultation broadcasts associated with a specific admission request.
     *
     * @param admissionRequestId the unique identifier of the admission request.
     * @return list of matching {@link AssessmentBroadcast} objects.
     */
    List<AssessmentBroadcast> findByAdmissionRequest_Id(UUID admissionRequestId);
}
