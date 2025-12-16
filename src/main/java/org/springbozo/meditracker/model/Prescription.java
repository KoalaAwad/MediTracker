package org.springbozo.meditracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springbozo.meditracker.model.embedded.Dosage;
import org.springbozo.meditracker.model.embedded.ScheduleEntry;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "prescription",
        indexes = {
                @Index(name = "idx_prescription_patient", columnList = "patient_id"),
                @Index(name = "idx_prescription_medicine", columnList = "medicine_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prescription_id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    // Explicit dosage value object
    @Embedded
    @NotNull
    private Dosage dosage;

    @Column(name = "start_date", nullable = false)
    @NotNull
    private LocalDate startDate; // inclusive

    @Column(name = "end_date")
    private LocalDate endDate; // optional, inclusive

    // IANA zone id (e.g., "Europe/London") used to render LocalTime correctly for the patient
    @Column(name = "time_zone", length = 64, nullable = false)
    @Size(max = 64)
    @NotNull
    private String timeZone;

    // Replaces weekDays + dayTimes. Each entry is (dayOfWeek, timeOfDay)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "prescription_schedule",
            joinColumns = @JoinColumn(name = "prescription_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"prescription_id", "day_of_week", "time_of_day"})
    )
    private Set<ScheduleEntry> schedule = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void validateDates() {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
    }
}