package com.frexsos.factory.repository;

import com.frexsos.factory.model.ProcessStep;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProcessStepRepository extends JpaRepository<ProcessStep, Long> {
    List<ProcessStep> findByProcessIdOrderByStepOrderAsc(Long processId);
    void deleteAllByProcessId(Long processId);
}
