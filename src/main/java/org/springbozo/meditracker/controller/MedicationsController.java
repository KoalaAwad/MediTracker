package org.springbozo.meditracker.controller;

import jakarta.transaction.Transactional;
import org.springbozo.meditracker.model.Medicine;
import org.springbozo.meditracker.service.MedicineService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/medicines")
public class MedicationsController {

    private final MedicineService medicineService;

    public MedicationsController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @GetMapping("/new")
    public String newMedicineForm(Model model) {
        model.addAttribute("medicine", new Medicine());
        return "medicines/medicine-form";
    }

    @PostMapping("/new")
    @Transactional
    public String saveMedicine(Medicine medicine) {
        medicineService.save(medicine);
        return "redirect:/prescriptions/new";
    }
}

