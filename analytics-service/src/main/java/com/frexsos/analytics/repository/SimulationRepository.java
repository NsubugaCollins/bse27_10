package com.frexsos.analytics.repository;

import com.frexsos.analytics.model.Simulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SimulationRepository extends JpaRepository<Simulation, Long> {

    List<Simulation> findByFactoryIdOrderByRunDateDesc(Long factoryId);

    @Query("SELECT AVG(s.totalDurationMinutes) FROM Simulation s WHERE s.factoryId = :factoryId")
    Double avgDurationByFactory(@Param("factoryId") Long factoryId);

    @Query("SELECT COUNT(s) FROM Simulation s WHERE s.factoryId = :factoryId")
    Long countByFactory(@Param("factoryId") Long factoryId);

    @Query("SELECT AVG(s.totalEnergyKWh) FROM Simulation s WHERE s.factoryId = :factoryId")
    Double avgEnergyByFactory(@Param("factoryId") Long factoryId);
}
