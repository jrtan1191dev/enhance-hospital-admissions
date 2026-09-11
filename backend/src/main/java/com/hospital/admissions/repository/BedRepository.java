package com.hospital.admissions.repository;

import com.hospital.admissions.entity.Bed;
import com.hospital.admissions.entity.BedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Bed} inventory entity operations.
 */
@Repository
public interface BedRepository extends JpaRepository<Bed, UUID> {

    /**
     * Finds all beds physically located within a given ward.
     *
     * @param wardId the unique identifier of the ward.
     * @return list of {@link Bed} entities in the ward.
     */
    List<Bed> findByWard_Id(UUID wardId);

    /**
     * Finds all beds matching a specific availability and occupancy status.
     *
     * @param status the {@link BedStatus} to filter by.
     * @return list of matching {@link Bed} entities.
     */
    List<Bed> findByStatus(BedStatus status);
}
