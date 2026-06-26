package com.evoting.evotingsystem.controller;

import com.evoting.evotingsystem.dto.LoginRequest;
import com.evoting.evotingsystem.dto.RegistrationRequest;
import com.evoting.evotingsystem.entity.User;
import com.evoting.evotingsystem.security.CustomAuthenticationSuccessHandler;
import com.evoting.evotingsystem.service.AuthService;
import com.evoting.evotingsystem.util.SessionKeys;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(name = "logout", required = false) String logout,
            @RequestParam(name = "expired", required = false) String expired,
            @RequestParam(name = "denied", required = false) String denied,
            Model model,
            HttpSession session,
            jakarta.servlet.http.HttpServletResponse response
    ) {
        // Prevent caching of login page to avoid stale form data
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        if (session.getAttribute(SessionKeys.AUTH_USER_ID) != null) {
            String role = (String) session.getAttribute(SessionKeys.AUTH_USER_ROLE);
            if ("ROLE_ADMIN".equals(role)) {
                return "redirect:/admin/dashboard";
            }
            return "redirect:/voter/dashboard";
        }

        // Always provide a fresh login request to prevent autofill persistence
        model.addAttribute("loginRequest", new LoginRequest());
        if (logout != null) {
            model.addAttribute("toastMessage", "Logout successful");
            model.addAttribute("toastType", "success");
        }
        if (expired != null) {
            model.addAttribute("toastMessage", "Session expired. Please login again.");
            model.addAttribute("toastType", "warning");
        }
        if (denied != null) {
            model.addAttribute("toastMessage", "Access denied for requested page.");
            model.addAttribute("toastType", "error");
        }
        model.addAttribute("pageTitle", "Login");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model, jakarta.servlet.http.HttpServletResponse response) {
        // Prevent caching of register page
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // Always provide a fresh registration request
        model.addAttribute("registrationRequest", new RegistrationRequest());
        model.addAttribute("pageTitle", "Register");
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registrationRequest") RegistrationRequest registrationRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Register");
            return "auth/register";
        }

        try {
            authService.registerVoter(registrationRequest);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("registrationError", ex.getMessage());
            model.addAttribute("pageTitle", "Register");
            return "auth/register";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Registration successful. Please login.");
        return "redirect:/login";
    }

    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
            BindingResult bindingResult,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Login");
            return "auth/login";
        }

        User authenticatedUser = authService.authenticateAndGetUser(loginRequest.getEmail(), loginRequest.getPassword());
        if (authenticatedUser == null) {
            model.addAttribute("loginError", "Invalid email or password.");
            model.addAttribute("pageTitle", "Login");
            return "auth/login";
        }

        session.setAttribute(SessionKeys.AUTH_USER_ID, authenticatedUser.getId());
        session.setAttribute(SessionKeys.AUTH_USERNAME, authenticatedUser.getUsername());
        session.setAttribute(SessionKeys.AUTH_USER_ROLE, "ROLE_" + authenticatedUser.getRole().name());

        redirectAttributes.addFlashAttribute("toastMessage", "Login successful");
        redirectAttributes.addFlashAttribute("toastType", "success");
        return "redirect:" + authenticationSuccessHandler.resolveRedirectPath(authenticatedUser.getRole());
    }

    // Logout is handled by Spring Security at /logout
}
