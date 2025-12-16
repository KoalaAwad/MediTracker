package org.springbozo.meditracker.model.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ScheduleEntry {

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 16, nullable = false)
    @NotNull
    private DayOfWeek dayOfWeek;

    @Column(name = "time_of_day", nullable = false)
    @NotNull
    private LocalTime timeOfDay;

    public ScheduleEntry(DayOfWeek dayOfWeek, LocalTime timeOfDay) {
        this.dayOfWeek = dayOfWeek;
        this.timeOfDay = timeOfDay;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduleEntry that)) return false;
        return dayOfWeek == that.dayOfWeek && Objects.equals(timeOfDay, that.timeOfDay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayOfWeek, timeOfDay);
    }
}

