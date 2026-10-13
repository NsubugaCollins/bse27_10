package com.frexsos.analytics.repository;

import com.frexsos.analytics.model.OperationalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OperationalLogRepository extends JpaRepository<OperationalLog, Long> {

    List<OperationalLog> findByFactoryIdOrderByRunDateDesc(Long factoryId);

    List<OperationalLog> findAllByOrderByRunDateDesc();

    @Query("SELECT o FROM OperationalLog o WHERE o.factoryId = :factoryId ORDER BY o.runDate DESC")
    List<OperationalLog> findRecentByFactory(@Param("factoryId") Long factoryId);

    @Query("SELECT AVG(o.totalEnergyKWh) FROM OperationalLog o WHERE o.factoryId = :factoryId")
    Double avgEnergyByFactory(@Param("factoryId") Long factoryId);

    @Query("SELECT AVG(o.avgCycleTimeMinutes) FROM OperationalLog o WHERE o.factoryId = :factoryId")
    Double avgCycleTimeByFactory(@Param("factoryId") Long factoryId);

    @Query("SELECT SUM(o.itemCount) FROM OperationalLog o WHERE o.factoryId = :factoryId")
    Long totalItemsByFactory(@Param("factoryId") Long factoryId);
}
