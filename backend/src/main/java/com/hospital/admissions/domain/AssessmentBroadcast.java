package com.hospital.admissions.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assessment_broadcasts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentBroadcast extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "admission_request_id", nullable = false)
    private AdmissionRequest admissionRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpecialtyCluster targetCluster;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BroadcastStatus status;

    private String claimedBySpecialistId;
    private LocalDateTime claimedAt;

    @Column(length = 2000)
    private String consultNotes;
}
