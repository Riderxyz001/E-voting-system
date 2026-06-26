package com.evoting.evotingsystem.controller;

import com.evoting.evotingsystem.dto.election.ActiveElectionsPageDto;
import com.evoting.evotingsystem.entity.Election;
import com.evoting.evotingsystem.repository.ElectionRepository;
import com.evoting.evotingsystem.service.ElectionService;
import com.evoting.evotingsystem.util.SessionKeys;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/voter")
@RequiredArgsConstructor
public class ElectionController {

    private final ElectionRepository electionRepository;
    private final ElectionService electionService;

    @GetMapping("/elections/{id}")
    public String viewElectionDetails(@PathVariable Long id, HttpSession session, Model model) {
        Election election = electionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Election not found"));

        Long userId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        ActiveElectionsPageDto pageData = electionService.getActiveElectionsPage(userId, null, "ALL", 0, 1);

        com.evoting.evotingsystem.entity.ElectionStatus calculatedStatus = electionService.calculateStatus(election);
        if (calculatedStatus == com.evoting.evotingsystem.entity.ElectionStatus.DRAFT) {
            return "redirect:/voter/active-elections";
        }

        String statusBadgeClass = switch (calculatedStatus) {
            case ACTIVE -> "text-bg-success-subtle text-success-emphasis";
            case UPCOMING -> "text-bg-warning-subtle text-warning-emphasis";
            case COMPLETED -> "text-bg-secondary text-light";
            default -> "text-bg-primary-subtle text-primary-emphasis";
        };

        model.addAttribute("pageTitle", "Election Details");
        model.addAttribute("election", election);
        model.addAttribute("pageData", pageData);
        model.addAttribute("activeMenu", "active-elections");
        model.addAttribute("statusBadgeClass", statusBadgeClass);
        return "voter/election-details";
    }
}
