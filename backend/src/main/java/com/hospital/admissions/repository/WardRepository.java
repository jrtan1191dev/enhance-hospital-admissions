package com.hospital.admissions.repository;

import com.hospital.admissions.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for inpatient {@link Ward} entities.
 */
@Repository
public interface WardRepository extends JpaRepository<Ward, UUID> {

    /**
     * Retrieves all inpatient wards ordered ascending by building level and ward name.
     *
     * @return ordered list of {@link Ward} entities.
     */
    List<Ward> findAllByOrderByLevelAscNameAsc();
}
