package com.evoting.evotingsystem.controller;

import com.evoting.evotingsystem.dto.vote.MyVotesPageDto;
import com.evoting.evotingsystem.service.VoteService;
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
public class MyVotesController {

    private final VoteService voteService;

    @GetMapping("/my-votes")
    public String myVotes(
            @RequestParam(name = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        Long userId = (Long) session.getAttribute(SessionKeys.AUTH_USER_ID);
        MyVotesPageDto pageData = voteService.getMyVotesPage(userId, page, 8);

        model.addAttribute("pageTitle", "My Votes");
        model.addAttribute("pageData", pageData);
        model.addAttribute("activeMenu", "my-votes");
        return "voter/my-votes";
    }
}
