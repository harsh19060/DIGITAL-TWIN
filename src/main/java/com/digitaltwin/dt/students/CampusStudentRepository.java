package com.digitaltwin.dt.students;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CampusStudentRepository extends JpaRepository<CampusStudent, Long> {
    Optional<CampusStudent> findByStudentCodeIgnoreCase(String studentCode);
}
