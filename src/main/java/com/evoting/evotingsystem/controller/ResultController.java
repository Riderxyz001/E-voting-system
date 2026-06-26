package com.evoting.evotingsystem.controller;

import com.evoting.evotingsystem.dto.result.ResultsPageDto;
import com.evoting.evotingsystem.service.ResultService;
import com.evoting.evotingsystem.util.SessionKeys;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/voter")
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    @GetMapping("/results")
    public String results(
            @RequestParam(name = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        Long userId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        ResultsPageDto pageData = resultService.getResultsPage(userId, page, 6);

        model.addAttribute("pageTitle", "Election Results");
        model.addAttribute("pageData", pageData);
        model.addAttribute("activeMenu", "results");
        return "voter/results";
    }

    @GetMapping("/results/{electionId}")
    public String resultsByElection(
            @PathVariable Long electionId,
            HttpSession session,
            Model model
    ) {
        Long userId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        ResultsPageDto pageData = resultService.getResultsPage(userId, 0, 6);

        model.addAttribute("pageTitle", "Election Results");
        model.addAttribute("focusElectionId", electionId);
        model.addAttribute("pageData", pageData);
        model.addAttribute("activeMenu", "results");
        return "voter/results";
    }

    @GetMapping("/results/{electionId}/download")
    public ResponseEntity<String> downloadResult(@PathVariable Long electionId) {
        String csv = resultService.exportElectionResultCsv(electionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"result-" + electionId + ".csv\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }
}
