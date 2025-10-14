package com.example.meditracker.mapper;

import com.example.meditracker.dto.CreateMedicineDto;
import com.example.meditracker.dto.MedicineDto;
import com.example.meditracker.entity.Medicine;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MedicineMapper {
    public MedicineDto toDto(Medicine entity){
        if (entity == null) return null;
        return new MedicineDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getDosageAmount(),
                entity.getDosageUnit(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public List<MedicineDto> toDtoList(List<Medicine> entities){
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Medicine toEntity(CreateMedicineDto createDto){
        if (createDto == null) return null;

        return new Medicine(
            createDto.getName(),
            createDto.getDescription(),
            createDto.getDosageAmount(),
            createDto.getDosageUnit()
        );
    }

    public void updateEntityFromDto(Medicine entity, CreateMedicineDto dto) {
        if (entity == null) return;
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setDosageAmount(dto.getDosageAmount());
        entity.setDosageUnit(dto.getDosageUnit());
    }
}
