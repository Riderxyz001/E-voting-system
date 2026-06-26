package com.evoting.evotingsystem.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/error")
public class AppErrorController implements ErrorController {

    @GetMapping("/403")
    public String accessDenied(Model model) {
        model.addAttribute("pageTitle", "Access Denied");
        return "error/403";
    }

    @GetMapping("/404")
    public String notFound(Model model) {
        model.addAttribute("pageTitle", "Page Not Found");
        return "error/404";
    }

    @GetMapping("/500")
    public String internalServerError(Model model) {
        model.addAttribute("pageTitle", "Server Error");
        return "error/500";
    }

    @RequestMapping
    public String handleError(HttpServletRequest request, Model model) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode != null) {
            int status = Integer.parseInt(statusCode.toString());
            if (status == HttpStatus.FORBIDDEN.value()) {
                return "error/403";
            }
            if (status == HttpStatus.NOT_FOUND.value()) {
                return "error/404";
            }
            if (status == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return "error/500";
            }
        }
        model.addAttribute("pageTitle", "Error");
        model.addAttribute("errorMessage", "Something went wrong. Please try again.");
        return "error/general";
    }
}

