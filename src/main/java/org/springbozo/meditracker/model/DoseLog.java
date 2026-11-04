package org.springbozo.meditracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springbozo.meditracker.model.enums.DoseStatus;

import java.time.Instant;

/**
 * One scheduled dose of a prescription. Created when the reminder goes out, or earlier
 * if the user logs it before its time; doses nobody touched have no row.
 */
@Entity
@Table(name = "dose_log")
@Getter
@Setter
public class DoseLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dose_id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DoseStatus status;

    @Column(name = "status_at")
    private Instant statusAt;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "followed_up_at")
    private Instant followedUpAt;
}
