package com.platform.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApplicationRequest {

    @NotNull(message = "Job ID is required")
    private Long jobId;

    @Size(max = 5000, message = "Cover letter must not exceed 5000 characters")
    private String coverLetter;

    @Size(max = 500, message = "Resume URL must not exceed 500 characters")
    private String resumeUrl;
}
