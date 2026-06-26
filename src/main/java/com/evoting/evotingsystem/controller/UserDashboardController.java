package com.evoting.evotingsystem.controller;

import com.evoting.evotingsystem.dto.dashboard.UserDashboardDto;
import com.evoting.evotingsystem.service.DashboardService;
import com.evoting.evotingsystem.util.SessionKeys;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/voter")
@RequiredArgsConstructor
public class UserDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        UserDashboardDto dashboard = dashboardService.getUserDashboard(userId);

        model.addAttribute("pageTitle", "Voter Dashboard");
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("pageData", dashboard);
        model.addAttribute("activeMenu", "dashboard");
        return "voter/dashboard";
    }
}
