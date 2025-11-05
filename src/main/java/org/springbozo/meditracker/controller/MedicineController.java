package org.springbozo.meditracker.controller;

import jakarta.transaction.Transactional;
import org.springbozo.meditracker.model.Medicine;
import org.springbozo.meditracker.repository.MedicineRepository;
import org.springbozo.meditracker.service.MedicineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("medicine")
public class MedicineController {


    private final MedicineService medicineService;

    @Autowired
    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @GetMapping("/{id}")
    public String viewMedicine(@PathVariable int id, Model model) {
        var medicineOpt = medicineService.getMedicineById(id);
        if (medicineOpt.isEmpty()) {
            return "redirect:/";
        }

        model.addAttribute("medicine", medicineOpt.get());
        return "prescription-detail";
    }

    @GetMapping
    public String listMedicines(Model model) {
        model.addAttribute("medicines", medicineService.listMedicines());
        return "prescription-all";
    }

    // New plural endpoints for adding medicines from the prescription flow
    @GetMapping(path = "/../medicines/new")
    public String newMedicineForm(Model model) {
        model.addAttribute("medicine", new Medicine());
        return "medicines/medicine-form";
    }

    @PostMapping(path = "/../medicines/new")
    @Transactional
    public String saveMedicine(Medicine medicine) {
        medicineService.save(medicine);
        return "redirect:/prescriptions/new";
    }

}
