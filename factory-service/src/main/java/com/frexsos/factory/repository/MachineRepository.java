package com.frexsos.factory.repository;

import com.frexsos.factory.model.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MachineRepository extends JpaRepository<Machine, Long> {
    List<Machine> findByFactoryId(Long factoryId);
    void deleteAllByFactoryId(Long factoryId);
}
