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
        return "medicine-detail";
    }

    @GetMapping("/all")
    public String listMedicines(Model model) {
        List<Medicine> medicines = medicineService.listMedicines();
        model.addAttribute("medicines",medicines );
        return "medicine-all";
    }


}
