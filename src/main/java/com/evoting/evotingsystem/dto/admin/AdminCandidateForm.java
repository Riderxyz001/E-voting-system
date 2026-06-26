package com.evoting.evotingsystem.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCandidateForm {
    @NotBlank(message = "Candidate name is required")
    @Size(min = 2, max = 100, message = "Candidate name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Party/Group is required")
    @Size(min = 2, max = 100, message = "Party/Group must be between 2 and 100 characters")
    private String partyName;

    @Size(max = 1000, message = "Manifesto can be up to 1000 characters")
    private String manifesto;

    @NotNull(message = "Please select an election")
    private Long electionId;

    private org.springframework.web.multipart.MultipartFile photo;
}
