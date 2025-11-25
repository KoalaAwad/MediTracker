package org.springbozo.meditracker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @GetMapping("/welcome")
    public String allAccess(){
        return "Everyone Access";
    }


    @GetMapping("/user")
    public String userAccess(){
        return "User Content with JWT";
    }


    @GetMapping("/special")
    public String specialAccess(){
        return "Special access with JWT";
    }
}
