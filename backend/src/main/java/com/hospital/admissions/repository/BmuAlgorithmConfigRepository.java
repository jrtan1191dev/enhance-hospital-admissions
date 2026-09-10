package com.hospital.admissions.repository;

import com.hospital.admissions.entity.BmuAlgorithmConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BmuAlgorithmConfigRepository extends JpaRepository<BmuAlgorithmConfig, UUID> {
}
