package com.platform.job.repository;

import com.platform.job.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByName(String name);

    boolean existsByName(String name);

    List<Company> findByIndustry(String industry);

    List<Company> findByNameContainingIgnoreCase(String keyword);

    List<Company> findByCreatedBy(Long userId);
}
