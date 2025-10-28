package org.springbozo.meditracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicController {

    @GetMapping(path ={"/", "/home"})
    public String home() {
        return "/public/home";
    }

    @GetMapping("/about")
    public String about() {
        return "/public/about";
    }

}
