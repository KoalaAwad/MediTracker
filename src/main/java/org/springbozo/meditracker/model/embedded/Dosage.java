package org.springbozo.meditracker.model.embedded;


import java.math.BigDecimal;

import org.springbozo.meditracker.model.enums.DosageUnit;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Embeddable
public class Dosage {

    @NotNull
    @Column(name = "dosage_unit", length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    private DosageUnit unit;

    @DecimalMin("0.01")
    @NotNull
    @Column(name = "dosage_amount", precision = 10, scale = 2, nullable = false)

    private BigDecimal amount;
}



