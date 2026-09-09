package com.hospital.admissions.repository;

import com.hospital.admissions.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Optional<Patient> findByQueueToken(String queueToken);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Patient p WHERE p.id NOT IN " +
            "(SELECT ar.patient.id FROM AdmissionRequest ar WHERE ar.status != com.hospital.admissions.domain.AdmissionStatus.DISCHARGED)")
    java.util.List<Patient> findPatientsWithoutActiveAdmission();
}
