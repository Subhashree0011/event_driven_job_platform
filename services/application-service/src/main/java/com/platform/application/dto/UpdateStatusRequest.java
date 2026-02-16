package com.platform.application.dto;

import com.platform.application.model.Application;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStatusRequest {

    @NotNull(message = "New status is required")
    private Application.ApplicationStatus status;

    private String notes;
}
