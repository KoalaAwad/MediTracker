package org.springbozo.meditracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Getter
@Setter
@Entity
@NoArgsConstructor
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medicine_id")
    private int id;

    @Column(name = "medicine_name")
    private String name;

    @Column(name = "generic_name")
    private String genericName;

    private String manufacturer;

    @Column(name = "dosage_form")
    private String dosageForm;

    private String strength;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "side_effects", columnDefinition = "text")
    private String sideEffects;

    @Column(columnDefinition = "text")
    private String contraindications;

    @Column(name = "active", nullable = false)
    private boolean active = true; // Default to active when creating new medicine

    // Preserve OpenFDA fields as JSONB map: key -> array of strings as in OpenFDA
    // Example: { "generic_name": ["IBUPROFEN"], "brand_name": ["ADVIL"], ... }
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "openfda", columnDefinition = "jsonb")
    private java.util.Map<String, java.util.List<String>> openfda;

}