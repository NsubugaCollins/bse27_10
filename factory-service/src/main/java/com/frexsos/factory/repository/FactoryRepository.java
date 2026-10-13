package com.frexsos.factory.repository;

import com.frexsos.factory.model.Factory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FactoryRepository extends JpaRepository<Factory, Long> {
}
