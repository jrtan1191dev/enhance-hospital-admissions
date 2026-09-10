package com.hospital.admissions.repository;

import com.hospital.admissions.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Optional<Patient> findByQueueToken(String queueToken);

    @Query("SELECT p FROM Patient p WHERE p.id NOT IN " +
            "(SELECT ar.patient.id FROM AdmissionRequest ar WHERE ar.status != com.hospital.admissions.entity.AdmissionStatus.DISCHARGED)")
    List<Patient> findPatientsWithoutActiveAdmission();
}
