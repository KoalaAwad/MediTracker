package org.springbozo.meditracker.repository;


import org.springbozo.meditracker.model.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    long countByActiveTrue();

    // Substring search on brand or generic name; lower(...) LIKE uses the pg_trgm indexes from V5
    @Query("select m from Medicine m where m.active = true and (lower(m.name) like :q or lower(m.genericName) like :q)")
    Page<Medicine> search(@Param("q") String likePattern, Pageable pageable);
}
