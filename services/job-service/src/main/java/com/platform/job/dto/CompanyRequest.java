package com.platform.job.dto;

import com.platform.job.model.Company;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String name;

    private String description;

    @Size(max = 500, message = "Website URL must not exceed 500 characters")
    private String website;

    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    private String logoUrl;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    private Company.CompanySize companySize;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;
}
