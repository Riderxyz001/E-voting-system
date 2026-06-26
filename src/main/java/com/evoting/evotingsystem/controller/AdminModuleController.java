package com.evoting.evotingsystem.controller;

import com.evoting.evotingsystem.dto.admin.AdminCandidateForm;
import com.evoting.evotingsystem.dto.admin.AdminElectionForm;
import com.evoting.evotingsystem.entity.NotificationType;
import com.evoting.evotingsystem.entity.User;
import com.evoting.evotingsystem.repository.projection.AdminVoteRowView;
import com.evoting.evotingsystem.service.AdminDashboardService;
import com.evoting.evotingsystem.service.AdminPanelService;
import com.evoting.evotingsystem.util.SessionKeys;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminModuleController {

    private final AdminPanelService adminPanelService;
    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Long adminId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        model.addAttribute("shell", adminPanelService.getShellData(adminId));
        model.addAttribute("dashboard", adminDashboardService.getDashboard(adminId));
        model.addAttribute("pageTitle", "Admin Dashboard");
        model.addAttribute("activeMenu", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/elections")
    public String elections(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", defaultValue = "ALL") String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        Long adminId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        model.addAttribute("shell", adminPanelService.getShellData(adminId));
        model.addAllAttributes(adminPanelService.getElectionsPage(q, status, page, 10));
        if (!model.containsAttribute("electionForm")) {
            model.addAttribute("electionForm", new AdminElectionForm());
        }
        model.addAttribute("pageTitle", "Elections");
        model.addAttribute("activeMenu", "elections");
        return "admin/elections";
    }

    @PostMapping("/elections/create")
    public String createElection(
            @Valid @ModelAttribute("electionForm") AdminElectionForm electionForm,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (!bindingResult.hasErrors() && electionForm.getEndsAt() != null && electionForm.getStartsAt() != null
                && !electionForm.getEndsAt().isAfter(electionForm.getStartsAt())) {
            bindingResult.rejectValue("endsAt", "endsAt.order", "End date must be after start date.");
        }
        if (bindingResult.hasErrors()) {
            Long adminId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
            model.addAttribute("shell", adminPanelService.getShellData(adminId));
            model.addAllAttributes(adminPanelService.getElectionsPage("", "ALL", 0, 10));
            model.addAttribute("pageTitle", "Elections");
            model.addAttribute("activeMenu", "elections");
            return "admin/elections";
        }
        adminPanelService.createElection(
                electionForm.getTitle(),
                electionForm.getDescription(),
                electionForm.getStartsAt(),
                electionForm.getEndsAt()
        );
        redirectAttributes.addFlashAttribute("successMessage", "Election created successfully.");
        return "redirect:/admin/elections";
    }

    @PostMapping("/elections/{id}/delete")
    public String deleteElection(@PathVariable Long id) {
        adminPanelService.deleteElection(id);
        return "redirect:/admin/elections";
    }

    @PostMapping("/elections/{id}/publish")
    public String publishElection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminPanelService.publishElection(id);
            redirectAttributes.addFlashAttribute("successMessage", "Election published successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/elections";
    }

    @GetMapping("/candidates")
    public String candidates(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        Long adminId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        model.addAttribute("shell", adminPanelService.getShellData(adminId));
        model.addAllAttributes(adminPanelService.getCandidatesPage(q, page, 10));
        model.addAttribute("electionsList", adminPanelService.getElectionsPage("", "ALL", 0, 200).get("elections"));
        if (!model.containsAttribute("candidateForm")) {
            model.addAttribute("candidateForm", new AdminCandidateForm());
        }
        model.addAttribute("pageTitle", "Candidates");
        model.addAttribute("activeMenu", "candidates");
        return "admin/candidates";
    }

    @PostMapping("/candidates/create")
    public String createCandidate(
            @Valid @ModelAttribute("candidateForm") AdminCandidateForm candidateForm,
            BindingResult bindingResult,
            HttpSession session,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            Long adminId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
            model.addAttribute("shell", adminPanelService.getShellData(adminId));
            model.addAllAttributes(adminPanelService.getCandidatesPage("", 0, 10));
            model.addAttribute("electionsList", adminPanelService.getElectionsPage("", "ALL", 0, 200).get("elections"));
            model.addAttribute("pageTitle", "Candidates");
            model.addAttribute("activeMenu", "candidates");
            return "admin/candidates";
        }
        adminPanelService.createCandidate(
                candidateForm.getFullName(),
                candidateForm.getPartyName(),
                candidateForm.getManifesto(),
                candidateForm.getElectionId(),
                candidateForm.getPhoto()
        );
        return "redirect:/admin/candidates";
    }

    @PostMapping("/candidates/{id}/delete")
    public String deleteCandidate(@PathVariable Long id) {
        adminPanelService.deleteCandidate(id);
        return "redirect:/admin/candidates";
    }

    @GetMapping("/voters")
    public String voters(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", defaultValue = "ALL") String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        Long adminId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        Page<User> voters = adminPanelService.getVotersPage(q, status, page, 10);
        model.addAttribute("shell", adminPanelService.getShellData(adminId));
        model.addAttribute("voters", voters);
        model.addAttribute("search", q == null ? "" : q);
        model.addAttribute("pageTitle", "Voters");
        model.addAttribute("activeMenu", "voters");
        return "admin/voters";
    }

    @GetMapping("/votes")
    public String votes(
            @RequestParam(name = "electionId", required = false) Long electionId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        Long adminId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        Page<AdminVoteRowView> votes = adminPanelService.getVotesPage(electionId, page, 12);
        model.addAttribute("shell", adminPanelService.getShellData(adminId));
        model.addAttribute("votes", votes);
        model.addAttribute("selectedElectionId", electionId);
        model.addAttribute("electionsList", adminPanelService.getElectionsPage("", "ALL", 0, 200).get("elections"));
        model.addAttribute("pageTitle", "Votes");
        model.addAttribute("activeMenu", "votes");
        return "admin/votes";
    }

    @GetMapping("/results")
    public String results(HttpSession session, Model model) {
        Long adminId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        model.addAttribute("shell", adminPanelService.getShellData(adminId));
        model.addAllAttributes(adminPanelService.getResultsPage());
        model.addAttribute("pageTitle", "Results");
        model.addAttribute("activeMenu", "results");
        return "admin/results";
    }

    @GetMapping("/notifications")
    public String notifications(
            @RequestParam(name = "type", defaultValue = "ALL") String type,
            @RequestParam(name = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        Long adminId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        model.addAttribute("shell", adminPanelService.getShellData(adminId));
        model.addAllAttributes(adminPanelService.getAdminNotificationsPage(adminId, type, page, 10));
        model.addAttribute("pageTitle", "Notifications");
        model.addAttribute("activeMenu", "notifications");
        return "admin/notifications";
    }

    @PostMapping("/notifications/send")
    public String sendNotification(
            @RequestParam String title,
            @RequestParam String message,
            @RequestParam NotificationType type,
            HttpSession session
    ) {
        Long adminId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        adminPanelService.sendNotificationToAllVoters(adminId, title, message, type);
        return "redirect:/admin/notifications";
    }

    @GetMapping("/reports")
    public String reports(HttpSession session, Model model) {
        Long adminId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        model.addAttribute("shell", adminPanelService.getShellData(adminId));
        model.addAllAttributes(adminPanelService.getReportsPage());
        model.addAttribute("pageTitle", "Reports");
        model.addAttribute("activeMenu", "reports");
        return "admin/reports";
    }

    @GetMapping("/users")
    public String users(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "role", defaultValue = "ALL") String role,
            @RequestParam(name = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        Long adminId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        model.addAttribute("shell", adminPanelService.getShellData(adminId));
        model.addAllAttributes(adminPanelService.getUsersPage(q, role, page, 10));
        model.addAttribute("pageTitle", "Users");
        model.addAttribute("activeMenu", "users");
        return "admin/users";
    }

    @PostMapping("/users/{id}/status")
    public String updateUserStatus(@PathVariable Long id, @RequestParam String status) {
        adminPanelService.updateUserStatus(id, status);
        return "redirect:/admin/users";
    }

}
