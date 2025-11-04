package org.springbozo.meditracker.model;

import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

public enum FrequencyType {
    INTERVAL,
    WEEKLY_PATTERN,
    PRN,
    OTHER
}