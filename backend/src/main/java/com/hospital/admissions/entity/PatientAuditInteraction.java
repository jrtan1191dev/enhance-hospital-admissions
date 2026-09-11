package com.hospital.admissions.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity capturing patient and family interactions on the public tracking portal
 * for compliance auditing and communication engagement analytics.
 */
@Entity
@Table(name = "patient_audit_interactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientAuditInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String token;

    private UUID admissionId;

    @Column(nullable = false)
    private String actionType;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
