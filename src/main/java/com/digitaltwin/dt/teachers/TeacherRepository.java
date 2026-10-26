package com.digitaltwin.dt.teachers;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByStaffCodeIgnoreCase(String staffCode);
}
