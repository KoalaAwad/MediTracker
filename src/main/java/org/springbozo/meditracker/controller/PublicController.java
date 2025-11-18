package org.springbozo.meditracker.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicController {

    @GetMapping(path ={"/", "/home"})
    public ResponseEntity<String> home(){
        return ResponseEntity.ok("Welcome to MediTracker! Your health, our priority.");
    }

    @GetMapping("/about")
    public String about() {
        return "/public/about";
    }

}
