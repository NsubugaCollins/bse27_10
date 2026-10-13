package com.frexsos.factory.repository;

import com.frexsos.factory.model.ProductionProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductionProcessRepository extends JpaRepository<ProductionProcess, Long> {
    List<ProductionProcess> findByFactoryId(Long factoryId);
    void deleteAllByFactoryId(Long factoryId);
}
