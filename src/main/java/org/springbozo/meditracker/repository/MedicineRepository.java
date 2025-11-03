package org.springbozo.meditracker.repository;


import org.springbozo.meditracker.model.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Integer> {}
