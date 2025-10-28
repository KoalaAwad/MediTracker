package org.springbozo.meditracker.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springbozo.meditracker.DAO.RegistrationDto;
import org.springbozo.meditracker.constants.Constants;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }


    @RequestMapping(value = "/login", method = {RequestMethod.GET, RequestMethod.POST})
    public String displayLoginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "register", required = false) String register,
            Model model) {

        String errorMessage = null;
        if (error != null) {
            errorMessage = "Username or Password is incorrect!";
        } else if (logout != null) {
            errorMessage = "You have been successfully logged out!";
        } else if (register != null) {
            if (register.equals("true")) {
                errorMessage = "Registration successful. Login with registered credentials!";
            } else {
                errorMessage = "Registration failed. Please try again!";
            }
        }
        model.addAttribute("errorMessage", errorMessage);
        return "auth/login";
    }

    @GetMapping("/logout")
    public String logoutPage(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/login?logout=true";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registration", new RegistrationDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("registration")RegistrationDto registrationDto,
                               BindingResult result) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        if(userService.register(registrationDto)){
            return "redirect:/login?register=false";
        }
        return "redirect:/login?register=true";
    }
}


