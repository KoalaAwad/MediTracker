package org.springbozo.meditracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cglib.core.Local;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;


    private LocalDateTime startDate;
    @Column(nullable = true)
    private LocalDateTime endDate;


    @Column(nullable = true)
    private LocalTime timeOfDay;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "schedule_days", joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "day_of_week")
    private List<DayOfWeek> daysOfWeek = new ArrayList<>();

    //CONDITION BASED
    @Column(nullable = true)
    private String conditionDescription;

    //INTERVAL BASED
    @Column(nullable = true)
    private Integer intervalDays;

}
