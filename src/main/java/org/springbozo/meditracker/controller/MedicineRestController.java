package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.model.Medicine;
import org.springbozo.meditracker.config.JwtUtil;
import org.springbozo.meditracker.service.MedicineService;
import org.springbozo.meditracker.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/medicine")
@CrossOrigin
public class MedicineRestController {

    private final MedicineService medicineService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public MedicineRestController(MedicineService medicineService, UserService userService, JwtUtil jwtUtil) {
        this.medicineService = medicineService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<?> getAllMedicines(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.ValidateJwtToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }

            List<Medicine> medicines = medicineService.listMedicines();
            return ResponseEntity.ok(medicines);

        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    // New paged endpoint
    @GetMapping("/paged")
    public ResponseEntity<?> getPagedMedicines(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.ValidateJwtToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }

            var pageResult = medicineService.listMedicinesPaged(page, size, q);
            return ResponseEntity.ok(Map.of(
                    "content", pageResult.getContent(),
                    "page", pageResult.getNumber(),
                    "size", pageResult.getSize(),
                    "totalElements", pageResult.getTotalElements(),
                    "totalPages", pageResult.getTotalPages()
            ));

        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMedicineById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int id) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.ValidateJwtToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }

            Optional<Medicine> medicine = medicineService.getMedicineById(id);
            if (medicine.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Medicine not found"));
            }

            return ResponseEntity.ok(medicine.get());

        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMedicine(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int id) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.ValidateJwtToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }

            String email = jwtUtil.getUserFromToken(token);
            if (!userService.hasAdminRole(email)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Admin access required"));
            }

            boolean deleted = medicineService.deleteMedicine(id);
            if (!deleted) {
                return ResponseEntity.status(404).body(Map.of("error", "Medicine not found"));
            }

            // Soft delete - medicine is marked as inactive but prescriptions remain intact
            return ResponseEntity.ok(Map.of(
                "message", "Medicine marked as inactive. Existing prescriptions are preserved."
            ));

        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/import")
    public ResponseEntity<?> importOpenFda(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody java.util.Map<String, Object> payload
    ) {
        try {
            // ===== SECURITY CHECKS =====

            // 1. Authentication check
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.ValidateJwtToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }

            String email = jwtUtil.getUserFromToken(token);
            if (!userService.hasAdminRole(email)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Admin access required"));
            }

            // 2. Validate payload structure
            if (payload == null || payload.isEmpty()) {
                return ResponseEntity.status(400).body(Map.of("error", "Empty payload"));
            }

            // 3. Limit maximum number of records (prevent DOS)
            Object resultsObj = payload.get("results");
            if (resultsObj == null) {
                return ResponseEntity.status(400).body(Map.of("error", "Invalid JSON structure: 'results' array required"));
            }


            int created = 0;
            int updated = 0;
            int skipped = 0;
            int skippedNoName = 0;
            int skippedError = 0;

            // Handle Drugs@FDA format: { "results": [...] }
            java.util.List<java.util.Map<String, Object>> records;

            if (resultsObj instanceof java.util.List) {
                //noinspection unchecked
                records = (java.util.List<java.util.Map<String, Object>>) resultsObj;
            } else {
                return ResponseEntity.status(400).body(Map.of("error", "Invalid 'results' format: must be an array"));
            }

            // 4. Limit max records to prevent DOS (adjust as needed)
            final int MAX_RECORDS = 50000;
            if (records.size() > MAX_RECORDS) {
                return ResponseEntity.status(400).body(Map.of("error", "Too many records: max " + MAX_RECORDS + " allowed"));
            }

            for (var rec : records) {
                try {
                    // Check if openfda is nested (Drugs@FDA format)
                    Object openfdaObj = rec.get("openfda");
                    java.util.Map<String, Object> sourceMap;

                    if (openfdaObj instanceof java.util.Map) {
                        //noinspection unchecked
                        sourceMap = (java.util.Map<String, Object>) openfdaObj;
                    } else {
                        // Flat structure (old format) or empty openfda
                        sourceMap = new java.util.HashMap<>();
                    }

                    // Build openfda map: collect only array-of-string fields we want
                    java.util.Map<String, java.util.List<String>> openfdaMap = new java.util.HashMap<>();

                    String[] keys = new String[]{"generic_name", "brand_name", "substance_name", "rxcui", "pharm_class_moa", "pharm_class_epc", "pharm_class_cs", "pharm_class_pe"};
                    for (String k : keys) {
                        Object v = sourceMap.get(k);
                        if (v instanceof java.util.List) {
                            //noinspection unchecked
                            java.util.List<Object> raw = (java.util.List<Object>) v;
                            java.util.List<String> strs = new java.util.ArrayList<>();
                            for (Object o : raw) {
                                if (o != null) strs.add(o.toString());
                            }
                            if (!strs.isEmpty()) openfdaMap.put(k, strs);
                        } else if (v instanceof String) {
                            openfdaMap.put(k, java.util.List.of((String) v));
                        }
                    }

                    // Determine display name with multiple fallbacks
                    String name = null;

                    // Try openfda.brand_name
                    if (openfdaMap.containsKey("brand_name") && !openfdaMap.get("brand_name").isEmpty())
                        name = openfdaMap.get("brand_name").get(0);

                    // Try openfda.generic_name
                    if (name == null && openfdaMap.containsKey("generic_name") && !openfdaMap.get("generic_name").isEmpty())
                        name = openfdaMap.get("generic_name").get(0);

                    // Try openfda.substance_name
                    if (name == null && openfdaMap.containsKey("substance_name") && !openfdaMap.get("substance_name").isEmpty())
                        name = openfdaMap.get("substance_name").get(0);

                    // Fallback: try to extract from products array
                    if (name == null) {
                        Object productsObj = rec.get("products");
                        if (productsObj instanceof java.util.List && !((java.util.List<?>) productsObj).isEmpty()) {
                            Object firstProduct = ((java.util.List<?>) productsObj).get(0);
                            if (firstProduct instanceof java.util.Map) {
                                Object brandName = ((java.util.Map<?, ?>) firstProduct).get("brand_name");
                                if (brandName instanceof String && !((String) brandName).isBlank()) {
                                    name = (String) brandName;
                                }
                                // Try generic_name from product
                                if (name == null) {
                                    Object genericName = ((java.util.Map<?, ?>) firstProduct).get("generic_name");
                                    if (genericName instanceof String && !((String) genericName).isBlank()) {
                                        name = (String) genericName;
                                    }
                                }
                            }
                        }
                    }

                    // Last resort: use application_number if available
                    if (name == null) {
                        Object appNum = rec.get("application_number");
                        if (appNum instanceof String && !((String) appNum).isBlank()) {
                            name = "Application " + appNum;
                        }
                    }

                    // Skip if still no name
                    if (name == null || name.isBlank()) {
                        skipped++;
                        skippedNoName++;
                        continue;
                    }

                    // 5. SANITIZE INPUT - prevent injection attacks
                    // Trim and limit length
                    name = name.trim();
                    if (name.length() > 500) {
                        name = name.substring(0, 500); // Truncate overly long names
                    }
                    // Remove potentially dangerous characters (keep alphanumeric, spaces, hyphens, parentheses, commas)
                    name = name.replaceAll("[^a-zA-Z0-9\\s\\-(),./]", "");

                    // Recheck after sanitization
                    if (name.isBlank()) {
                        skipped++;
                        skippedNoName++;
                        continue;
                    }

                    // Extract and sanitize manufacturer BEFORE deduplication check
                    String manufacturer = null;
                    Object manu = sourceMap.get("manufacturer_name");
                    if (manu == null) manu = rec.get("sponsor_name");
                    if (manu instanceof java.util.List && !((java.util.List<?>) manu).isEmpty()) {
                        manufacturer = ((java.util.List<?>) manu).get(0).toString().trim();
                        if (manufacturer.length() > 500) manufacturer = manufacturer.substring(0, 500);
                        manufacturer = manufacturer.replaceAll("[^a-zA-Z0-9\\s\\-(),./&]", "");
                        if (manufacturer.isBlank()) manufacturer = null;
                    } else if (manu instanceof String) {
                        manufacturer = ((String) manu).trim();
                        if (manufacturer.length() > 500) manufacturer = manufacturer.substring(0, 500);
                        manufacturer = manufacturer.replaceAll("[^a-zA-Z0-9\\s\\-(),./&]", "");
                        if (manufacturer.isBlank()) manufacturer = null;
                    }

                    // Check if medicine already exists (deduplication by name + manufacturer)
                    // This allows same drug from different manufacturers
                    if (medicineService.existsByNameAndManufacturer(name, manufacturer)) {
                        skipped++;
                        continue; // Skip duplicate
                    }

                    Medicine med = new Medicine();
                    med.setName(name);

                    if (manufacturer != null) {
                        med.setManufacturer(manufacturer);
                    }

                    if (openfdaMap.containsKey("generic_name") && !openfdaMap.get("generic_name").isEmpty()) {
                        String genericName = openfdaMap.get("generic_name").get(0).trim();
                        if (genericName.length() > 500) genericName = genericName.substring(0, 500);
                        genericName = genericName.replaceAll("[^a-zA-Z0-9\\s\\-(),./]", "");
                        med.setGenericName(genericName);
                    }


                    med.setOpenfda(openfdaMap.isEmpty() ? null : openfdaMap);

                    // Save (will create new row)
                    medicineService.save(med);
                    created++;

                } catch (Exception e) {
                    skipped++;
                    skippedError++;
                }
            }

            return ResponseEntity.ok(Map.of(
                    "created", created,
                    "updated", updated,
                    "skipped", skipped,
                    "skippedNoName", skippedNoName,
                    "skippedError", skippedError
            ));

        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + ex.getMessage()));
        }
    }
}
