package com.evoting.evotingsystem.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminAuthController {

    @GetMapping("/admin/login")
    public String adminLoginPage(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("toastMessage", "Use common login for Admin and Student.");
        redirectAttributes.addFlashAttribute("toastType", "warning");
        return "redirect:/login";
    }

    @PostMapping("/admin/login")
    public String adminLoginPost() {
        return "redirect:/login";
    }
}
