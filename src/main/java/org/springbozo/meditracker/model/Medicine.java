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

    @Column(name = "medicine_name", nullable = false)
    private String name;

    @Column(name = "generic_name")
    private String genericName;

    private String manufacturer;

    @Column(name = "active", nullable = false)
    private boolean active = true; // Default to active when creating new medicine

    // OpenFDA data as JSONB: contains brand_name, generic_name, substance_name,
    // rxcui, pharm_class_* arrays exactly as provided by FDA
    // From the openFDA NDC directory; null for medicines added by hand
    @Column(name = "product_ndc")
    private String productNdc;

    // e.g. "1 mg" or "5 mg / 10 mg" for combination products
    private String strength;

    @Column(name = "dosage_form")
    private String dosageForm;

    private String route;

    @Column(name = "product_type")
    private String productType;

    // DailyMed label id, used for the package label image and pill description
    @Column(name = "spl_set_id")
    private String splSetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "openfda", columnDefinition = "jsonb")
    private java.util.Map<String, java.util.List<String>> openfda;

}