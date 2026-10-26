package com.digitaltwin.dt.sections;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<StudentSection, Long> {
    Optional<StudentSection> findByCodeIgnoreCase(String code);
}
