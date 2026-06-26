package com.evoting.evotingsystem.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        model.addAttribute("pageTitle", "Error");
        model.addAttribute("errorMessage", ex.getMessage() == null ? "Something went wrong." : ex.getMessage());
        return "error/general";
    }
}
