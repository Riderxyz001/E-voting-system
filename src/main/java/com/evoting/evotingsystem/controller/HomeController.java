package com.evoting.evotingsystem.controller;

import com.evoting.evotingsystem.dto.dashboard.HomeDashboardDto;
import com.evoting.evotingsystem.service.DashboardService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DashboardService dashboardService;

    @GetMapping({"/", "/home"})
    public String home(HttpSession session, Model model) {
        HomeDashboardDto dashboard = dashboardService.getHomeDashboard();
        model.addAttribute("pageTitle", "E-Voting System");
        model.addAttribute("dashboard", dashboard);

        Long userId = (Long) session.getAttribute(com.evoting.evotingsystem.util.SessionKeys.AUTH_USER_ID);
        if (userId != null) {
            try {
                model.addAttribute("pageData", dashboardService.getUserDashboard(userId));
            } catch (Exception ignored) {
            }
        }

        return "home";
    }

    @GetMapping("/api/home/dashboard")
    @ResponseBody
    public HomeDashboardDto dashboardApi() {
        return dashboardService.getHomeDashboard();
    }
}
