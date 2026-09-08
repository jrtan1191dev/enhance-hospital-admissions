package com.hospital.admissions.repository;

import com.hospital.admissions.domain.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WardRepository extends JpaRepository<Ward, UUID> {
    List<Ward> findAllByOrderByLevelAscNameAsc();
}
