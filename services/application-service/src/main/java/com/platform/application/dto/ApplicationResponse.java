package com.platform.application.dto;

import com.platform.application.model.Application;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {

    private Long id;
    private Long jobId;
    private Long userId;
    private Application.ApplicationStatus status;
    private String coverLetter;
    private String resumeUrl;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
