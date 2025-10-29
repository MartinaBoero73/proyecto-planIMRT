package com.planimrt.controllers;

import com.planimrt.repository.UserRepository;
import com.planimrt.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final UserService userService;

    public HomeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/home")
    public String home(Model model) {
        String username = userService.getLoggedUserDTO().getUsername();
        model.addAttribute("username", username);
        return "home";
    }
}
