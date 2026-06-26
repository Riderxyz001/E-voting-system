package com.evoting.evotingsystem.controller;

import com.evoting.evotingsystem.dto.election.ActiveElectionsPageDto;
import com.evoting.evotingsystem.service.ElectionService;
import com.evoting.evotingsystem.util.SessionKeys;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/voter")
@RequiredArgsConstructor
public class ActiveElectionController {

    private final ElectionService electionService;

    @GetMapping("/active-elections")
    public String activeElections(
            @RequestParam(name = "q", required = false) String search,
            @RequestParam(name = "status", defaultValue = "ALL") String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        Long userId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        ActiveElectionsPageDto pageData = electionService.getActiveElectionsPage(userId, search, status, page, 9);

        model.addAttribute("pageTitle", "Active Elections");
        model.addAttribute("pageData", pageData);
        model.addAttribute("activeMenu", "active-elections");
        return "voter/active-elections";
    }
}
