package com.hospital.admissions.repository;

import com.hospital.admissions.domain.Bed;
import com.hospital.admissions.domain.BedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BedRepository extends JpaRepository<Bed, UUID> {
    List<Bed> findByWard_Id(UUID wardId);
    List<Bed> findByStatus(BedStatus status);
}
