package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.model.Person;
import org.springbozo.meditracker.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserController {

    @Autowired
    private PersonService userService;

    @GetMapping("user/new")
    public String showUser(Model model) {
        model.addAttribute("username", new Person());
        return "AddPerson.html";
    }

    @PostMapping
    public String addUser(Model model) {
        return "redirect:/";
    }
}
