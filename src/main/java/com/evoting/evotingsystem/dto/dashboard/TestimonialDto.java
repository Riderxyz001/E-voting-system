package com.evoting.evotingsystem.dto.dashboard;

public record TestimonialDto(
        Long id,
        String authorName,
        String designation,
        String message,
        Integer rating
) {
}
