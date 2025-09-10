package com.example.meditracker.controller;


import com.example.meditracker.entity.Medicine;
import com.example.meditracker.repository.MedicineRepository;
import com.example.meditracker.service.MedicineService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    @Autowired
    private MedicineService medicineService;

    @GetMapping
    public ResponseEntity<List<Medicine>> getAllMedicines(){
        List<Medicine> medicines = medicineService.getAllMedicines();
        return  ResponseEntity.ok(medicines);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medicine> getMedicineById(@PathVariable Long id){
        return medicineService.getMedicineById(id)
                .map(medicine -> ResponseEntity.ok(medicine))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Medicine> createMedicineById(@Valid @RequestBody Medicine medicine){
        try {
            Medicine createdMedicine = medicineService.createMedicine(medicine);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdMedicine);
        } catch (IllegalArgumentException e) {  // More specific first
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {          // More general second
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Medicine> updateMedicine(@PathVariable Long id,
                                                   @Valid @RequestBody Medicine medicine){
        try {
            Medicine updatedMedicine = medicineService.updateMedicine(id, medicine);
            return ResponseEntity.ok(updatedMedicine);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteMedicine(@PathVariable Long id){
        try {
            medicineService.deleteMedicine(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e){
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Medicine>> searchMedicines(@RequestParam String name){
        List<Medicine> medicines = medicineService.searchMedicinesByName(name);
        return ResponseEntity.ok(medicines);
    }


}
