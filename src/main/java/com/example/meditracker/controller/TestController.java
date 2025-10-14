package com.example.meditracker.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/test")
    public String test(){
        return "MediTracker API is running!";
    }

    @GetMapping("/health")
    public String health(){
        return "Application is healthy!";
    }
}
