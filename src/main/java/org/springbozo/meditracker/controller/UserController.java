package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.model.Person;
import org.springbozo.meditracker.repository.PersonRepository;
import org.springbozo.meditracker.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("person")
public class PersonController {

    private final PersonService personService;

    @Autowired
    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping("/display")
    public String showPerson(Model model) {
        model.addAttribute("person", new Person());
        return "/";
    }

    @PostMapping("/save")
    public String savePerson(@ModelAttribute("person") Person person) {
        personService.savePerson(person);
        return "redirect:/";
    }
}
