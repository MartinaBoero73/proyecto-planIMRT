package com.planimrt.controllers;

import com.planimrt.forms.UserForm;
import com.planimrt.model.UserRole;
import com.planimrt.services.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LoginController {

    private final UserService userService;

    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("userForm", new UserForm());
        model.addAttribute("roles", UserRole.values());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute("userForm") UserForm form,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("roles", UserRole.values());
            return "register";
        }

        if (!form.getPassword().equals(form.getPasswordConfirm())) {
            model.addAttribute("error", "Las contraseñas no coinciden");
            model.addAttribute("roles", UserRole.values());
            return "register";
        }

        try {
            userService.registerUser(form.getUsername(), form.getPassword(), form.getRole());
            model.addAttribute("success", "Usuario registrado correctamente");
            return "login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", UserRole.values());
            return "register";
        }
    }
}