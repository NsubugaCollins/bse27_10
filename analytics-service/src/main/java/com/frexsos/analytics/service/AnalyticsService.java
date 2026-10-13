package com.frexsos.analytics.service;

import com.frexsos.analytics.dto.FactoryAnalyticsDTO;
import com.frexsos.analytics.dto.PlatformSummaryDTO;
import com.frexsos.analytics.dto.ProductionTrendDTO;
import com.frexsos.analytics.model.OperationalLog;
import com.frexsos.analytics.model.Simulation;
import com.frexsos.analytics.repository.OperationalLogRepository;
import com.frexsos.analytics.repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OperationalLogRepository logRepository;
    private final SimulationRepository simulationRepository;

    // ── Factory-level KPIs ───────────────────────────────────────────────────

    public FactoryAnalyticsDTO getFactoryAnalytics(Long factoryId) {
        Long totalSims = simulationRepository.countByFactory(factoryId);
        Long totalRuns = (long) logRepository.findByFactoryIdOrderByRunDateDesc(factoryId).size();
        Long totalItems = logRepository.totalItemsByFactory(factoryId);
        Double avgCycle = logRepository.avgCycleTimeByFactory(factoryId);
        Double avgEnergy = logRepository.avgEnergyByFactory(factoryId);
        Double avgSimDuration = simulationRepository.avgDurationByFactory(factoryId);

        // OEE estimate: ratio of actual items/hr to theoretical max
        double oee = estimateOee(avgCycle, avgEnergy);

        // Find most common bottleneck
        String topBottleneck = findTopBottleneck(factoryId);

        return FactoryAnalyticsDTO.builder()
                .factoryId(factoryId)
                .totalSimulations(totalSims != null ? totalSims : 0L)
                .totalProductionRuns(totalRuns)
                .totalItemsProduced(totalItems != null ? totalItems : 0L)
                .avgCycleTimeMinutes(avgCycle != null ? round(avgCycle) : 0.0)
                .avgEnergyKWh(avgEnergy != null ? round(avgEnergy) : 0.0)
                .avgSimulationDurationMinutes(avgSimDuration != null ? round(avgSimDuration) : 0.0)
                .oeePercent(round(oee))
                .topBottleneck(topBottleneck)
                .build();
    }

    // ── Platform-wide summary ────────────────────────────────────────────────

    public PlatformSummaryDTO getPlatformSummary() {
        long totalSims = simulationRepository.count();
        List<OperationalLog> allLogs = logRepository.findAll();
        long totalRuns = allLogs.size();

        long totalItems = allLogs.stream()
                .mapToLong(l -> l.getItemCount() != null ? l.getItemCount() : 0)
                .sum();

        double avgEnergy = allLogs.stream()
                .mapToDouble(l -> l.getTotalEnergyKWh() != null ? l.getTotalEnergyKWh() : 0)
                .average().orElse(0);

        double avgCycle = allLogs.stream()
                .mapToDouble(l -> l.getAvgCycleTimeMinutes() != null ? l.getAvgCycleTimeMinutes() : 0)
                .average().orElse(0);

        long totalFactories = allLogs.stream()
                .map(OperationalLog::getFactoryId)
                .distinct().count();

        return PlatformSummaryDTO.builder()
                .totalFactories(totalFactories)
                .totalSimulations(totalSims)
                .totalProductionRuns(totalRuns)
                .totalItemsProduced(totalItems)
                .avgEnergyKWh(round(avgEnergy))
                .avgCycleTimeMinutes(round(avgCycle))
                .platformOeePercent(round(estimateOee(avgCycle, avgEnergy)))
                .build();
    }

    // ── Production trends ────────────────────────────────────────────────────

    public List<ProductionTrendDTO> getProductionTrends(Long factoryId) {
        List<OperationalLog> logs = factoryId != null
                ? logRepository.findByFactoryIdOrderByRunDateDesc(factoryId)
                : logRepository.findAllByOrderByRunDateDesc();

        return logs.stream().map(l -> ProductionTrendDTO.builder()
                .id(l.getId())
                .factoryId(l.getFactoryId())
                .runName(l.getName())
                .runDate(l.getRunDate())
                .itemCount(l.getItemCount())
                .totalDurationMinutes(l.getTotalDurationMinutes())
                .totalEnergyKWh(l.getTotalEnergyKWh())
                .avgCycleTimeMinutes(l.getAvgCycleTimeMinutes())
                .bottleneck(l.getBottleneck())
                .build()
        ).collect(Collectors.toList());
    }

    // ── Energy analytics ─────────────────────────────────────────────────────

    public List<ProductionTrendDTO> getEnergyTrends(Long factoryId) {
        return getProductionTrends(factoryId); // same data, filtered by caller
    }

    // ── Bottleneck frequency report ──────────────────────────────────────────

    public Map<String, Long> getBottleneckReport(Long factoryId) {
        List<OperationalLog> logs = factoryId != null
                ? logRepository.findByFactoryIdOrderByRunDateDesc(factoryId)
                : logRepository.findAll();

        return logs.stream()
                .filter(l -> l.getBottleneck() != null && !l.getBottleneck().isBlank())
                .collect(Collectors.groupingBy(OperationalLog::getBottleneck, Collectors.counting()));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private double estimateOee(Double avgCycleTimeMinutes, Double avgEnergyKWh) {
        // Simple OEE proxy: lower cycle time and energy → higher OEE
        if (avgCycleTimeMinutes == null || avgCycleTimeMinutes <= 0) return 75.0;
        // Baseline cycle time assumed at 5 min/item → OEE scales inversely
        double availabilityProxy = Math.min(1.0, 5.0 / avgCycleTimeMinutes);
        double energyEfficiency = avgEnergyKWh != null && avgEnergyKWh > 0
                ? Math.min(1.0, 10.0 / avgEnergyKWh) : 0.85;
        return Math.min(99.0, Math.max(30.0, availabilityProxy * energyEfficiency * 100));
    }

    private String findTopBottleneck(Long factoryId) {
        Map<String, Long> freq = getBottleneckReport(factoryId);
        return freq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None identified");
    }

    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
