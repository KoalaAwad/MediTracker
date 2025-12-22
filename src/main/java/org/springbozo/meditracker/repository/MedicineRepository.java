package org.springbozo.meditracker.repository;


import org.springbozo.meditracker.model.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Integer> {

    // Find only active medicines
    List<Medicine> findByActiveTrue();

    // Find by ID only if active
    Optional<Medicine> findByIdAndActiveTrue(int id);

    // Pagination: list only active medicines paged
    Page<Medicine> findByActiveTrue(Pageable pageable);

    // Simple search by medicine name or generic name (case-insensitive)
    Page<Medicine> findByActiveTrueAndNameContainingIgnoreCaseOrActiveTrueAndGenericNameContainingIgnoreCase(
            String nameQuery, Pageable pageable, String genericQuery);

    // Check if medicine exists by name (for deduplication during import)
    boolean existsByNameIgnoreCase(String name);
}
