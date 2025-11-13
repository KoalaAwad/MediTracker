package org.springbozo.meditracker.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
public class PublicController {

    @GetMapping(path ={"/", "/home"})
    public Map<String, String> home() {
        Map<String, String> response = new HashMap<>();
        response.put("title", "Welcome to MediTracker");
        response.put("message", "Migrating front end to ReactJS");
        return response;
    }

    @GetMapping("/about")
    public Map<String, String> about() {
        Map<String, String> response = new HashMap<>();
        response.put("title", "About MediTracker");
        response.put("message", "We track your medicines safely.");
        return response;
    }

}
