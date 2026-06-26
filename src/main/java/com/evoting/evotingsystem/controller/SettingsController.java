package com.evoting.evotingsystem.controller;

import com.evoting.evotingsystem.service.AdminPanelService;
import com.evoting.evotingsystem.service.SettingsService;
import com.evoting.evotingsystem.util.SessionKeys;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final AdminPanelService adminPanelService;

    @GetMapping
    public String settings(HttpSession session, Model model) {
        Long adminId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        model.addAttribute("shell", adminPanelService.getShellData(adminId));
        model.addAttribute("settings", settingsService.getSettings());
        model.addAttribute("pageTitle", "Settings");
        model.addAttribute("activeMenu", "settings");
        return "admin/settings";
    }

    @PostMapping("/save")
    public String saveSetting(
            @RequestParam String settingKey,
            @RequestParam String settingValue
    ) {
        settingsService.saveSetting(settingKey, settingValue);
        return "redirect:/admin/settings";
    }
}
