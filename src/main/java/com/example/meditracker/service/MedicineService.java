package com.example.meditracker.service;

import com.example.meditracker.dto.CreateMedicineDto;
import com.example.meditracker.dto.MedicineDto;
import com.example.meditracker.entity.Medicine;
import com.example.meditracker.mapper.MedicineMapper;
import com.example.meditracker.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MedicineService {

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private MedicineMapper medicineMapper;


    public List<MedicineDto> getAllMedicines() {
        List<Medicine> entities = medicineRepository.findAllByOrderByNameAsc();
        return medicineMapper.toDtoList(entities);
    }

    public Optional<MedicineDto> getMedicineById(Long id) {
        return medicineRepository.findById(id)
                .map(medicineMapper::toDto);
    }

    public MedicineDto createMedicine(CreateMedicineDto createDto) {

        validateMedicineDto(createDto);

        Medicine entity = medicineMapper.toEntity(createDto);
        Medicine savedEntity = medicineRepository.save(entity);

        return medicineMapper.toDto(savedEntity);
    }

    public Optional<MedicineDto> updateMedicine(Long id, CreateMedicineDto updateDto) {
        validateMedicineDto(updateDto);

        return medicineRepository.findById(id)
                .map(entity -> {
                    medicineMapper.updateEntityFromDto(entity, updateDto);
                    entity.setUpdatedAt(LocalDateTime.now());
                    Medicine savedEntity = medicineRepository.save(entity);
                    return medicineMapper.toDto(savedEntity);
                });
    }

    public boolean deleteMedicine(Long id) {
        if (medicineRepository.existsById(id)) {
            medicineRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<MedicineDto> searchMedicinesByName(String name) {
        List<Medicine> entities = medicineRepository.findByNameContainingIgnoreCase(name);
        return medicineMapper.toDtoList(entities);
    }

    private void validateMedicineDto(CreateMedicineDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Medicine name cannot be empty");
        }
        if (dto.getDosageAmount() == null || dto.getDosageAmount() <= 0) {
            throw new IllegalArgumentException("Dosage amount must be positive");
        }
        if (dto.getDosageUnit() == null || dto.getDosageUnit().trim().isEmpty()) {
            throw new IllegalArgumentException("Dosage unit cannot be empty");
        }
    }
}
