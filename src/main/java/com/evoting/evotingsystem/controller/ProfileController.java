package com.evoting.evotingsystem.controller;

import com.evoting.evotingsystem.dto.profile.ProfilePageDto;
import com.evoting.evotingsystem.dto.profile.UpdateProfileRequest;
import com.evoting.evotingsystem.service.UserService;
import com.evoting.evotingsystem.util.SessionKeys;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/voter")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        populateProfileModel(userId, model);
        return "voter/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @Valid @ModelAttribute("updateProfileRequest") UpdateProfileRequest updateProfileRequest,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        if (bindingResult.hasErrors()) {
            populateProfileModel(userId, model);
            return "voter/profile";
        }

        try {
            userService.updateProfile(userId, updateProfileRequest);
            redirectAttributes.addFlashAttribute("toastMessage", "Profile updated successfully");
            redirectAttributes.addFlashAttribute("toastType", "success");
        } catch (IllegalArgumentException ex) {
            populateProfileModel(userId, model);
            model.addAttribute("profileError", ex.getMessage());
            return "voter/profile";
        }
        return "redirect:/voter/profile";
    }

    private void populateProfileModel(Long userId, Model model) {
        ProfilePageDto pageData = userService.getProfilePage(userId);
        if (!model.containsAttribute("updateProfileRequest")) {
            model.addAttribute("updateProfileRequest", userService.getUpdateProfileRequest(userId));
        }
        model.addAttribute("pageTitle", "My Profile");
        model.addAttribute("activeMenu", "profile");
        model.addAttribute("pageData", pageData);
    }
}
