package com.evoting.evotingsystem.controller;

import com.evoting.evotingsystem.dto.election.ActiveElectionsPageDto;
import com.evoting.evotingsystem.entity.Candidate;
import com.evoting.evotingsystem.entity.Election;
import com.evoting.evotingsystem.entity.ElectionStatus;
import com.evoting.evotingsystem.repository.ElectionRepository;
import com.evoting.evotingsystem.service.ElectionService;
import com.evoting.evotingsystem.service.VoteService;
import com.evoting.evotingsystem.util.SessionKeys;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/voter")
@RequiredArgsConstructor
public class VoteController {

    private final ElectionRepository electionRepository;
    private final ElectionService electionService;
    private final VoteService voteService;

    @GetMapping("/elections/{id}/vote")
    public String votePage(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Election election = electionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Election not found"));

        Long userId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        // PageData is needed for topbar/sidebar user profile and navigation
        ActiveElectionsPageDto pageData = electionService.getActiveElectionsPage(userId, null, "ALL", 0, 1);

        // Security check: Only active elections allow voting
        if (electionService.calculateStatus(election) != ElectionStatus.ACTIVE) {
            redirectAttributes.addFlashAttribute("toastMessage", "Voting is only allowed for active elections.");
            redirectAttributes.addFlashAttribute("toastType", "error");
            return "redirect:/voter/elections/" + id;
        }

        // Check if already voted
        boolean alreadyVoted = voteService.hasVoted(userId, id);

        List<Candidate> candidates = election.getCandidates().stream()
                .sorted(Comparator.comparing(Candidate::getFullName))
                .toList();

        model.addAttribute("pageTitle", "Cast Your Vote");
        model.addAttribute("election", election);
        model.addAttribute("candidates", candidates);
        model.addAttribute("pageData", pageData);
        model.addAttribute("alreadyVoted", alreadyVoted);
        model.addAttribute("activeMenu", "active-elections");
        
        return "voter/vote-page";
    }

    @PostMapping("/elections/{id}/vote")
    public String castVote(@PathVariable Long id, @RequestParam("candidateId") Long candidateId, 
                           HttpSession session, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);

        try {
            voteService.castVote(userId, id, candidateId);
            redirectAttributes.addFlashAttribute("toastMessage", "Your vote has been cast successfully!");
            redirectAttributes.addFlashAttribute("toastType", "success");
            return "redirect:/voter/my-votes";
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("toastMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("toastType", "error");
            return "redirect:/voter/elections/" + id + "/vote";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("toastMessage", "An unexpected error occurred while casting your vote.");
            redirectAttributes.addFlashAttribute("toastType", "error");
            return "redirect:/voter/elections/" + id + "/vote";
        }
    }
}
