package org.springbozo.meditracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    @GetMapping({"/profile/dashboard", "dashboard"})
    public String dashboard() {
        return "profile/dashboard";
    }
}
