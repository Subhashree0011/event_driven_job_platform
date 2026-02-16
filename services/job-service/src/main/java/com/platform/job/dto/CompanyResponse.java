package com.platform.job.dto;

import com.platform.job.model.Company;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyResponse {

    private Long id;
    private String name;
    private String description;
    private String website;
    private String logoUrl;
    private String industry;
    private Company.CompanySize companySize;
    private String location;
    private Long createdBy;
    private LocalDateTime createdAt;
}
