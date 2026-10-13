package com.frexsos.factory.repository;

import com.frexsos.factory.model.Robot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RobotRepository extends JpaRepository<Robot, Long> {
    List<Robot> findByFactoryId(Long factoryId);
    void deleteAllByFactoryId(Long factoryId);
}
