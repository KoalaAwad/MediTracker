package com.example.meditracker.controller;


import com.example.meditracker.dto.CreateMedicineDto;
import com.example.meditracker.dto.MedicineDto;
import com.example.meditracker.entity.Medicine;
import com.example.meditracker.service.MedicineService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    @Autowired
    private MedicineService medicineService;

    @GetMapping
    public ResponseEntity<List<MedicineDto>> getAllMedicines(){
        List<MedicineDto> medicines = medicineService.getAllMedicines();
        return  ResponseEntity.ok(medicines);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicineDto> getMedicineById(@PathVariable Long id){
        return medicineService.getMedicineById(id)
                .map(medicine -> ResponseEntity.ok(medicine))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MedicineDto> createMedicineById(@Valid @RequestBody CreateMedicineDto createDto){
        try {
            MedicineDto createdMedicine = medicineService.createMedicine(createDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdMedicine);
        } catch (IllegalArgumentException e) {  // More specific first
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {          // More general second
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicineDto> updateMedicine(@PathVariable Long id,
                                                   @Valid @RequestBody CreateMedicineDto updateDto){
        try{
            return medicineService.updateMedicine(id, updateDto)
                    .map(medicine -> ResponseEntity.ok(medicine))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteMedicine(@PathVariable Long id){
        if (medicineService.deleteMedicine(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<MedicineDto>> searchMedicines(@RequestParam String name){
        List<MedicineDto> medicines = medicineService.searchMedicinesByName(name);
        return ResponseEntity.ok(medicines);
    }


}
