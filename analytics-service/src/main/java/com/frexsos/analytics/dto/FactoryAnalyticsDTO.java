package com.frexsos.analytics.dto;

import lombok.*;

/**
 * Summary KPIs for a single factory.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactoryAnalyticsDTO {
    private Long factoryId;
    private Long totalSimulations;
    private Long totalProductionRuns;
    private Long totalItemsProduced;
    private Double avgCycleTimeMinutes;
    private Double avgEnergyKWh;
    private Double avgSimulationDurationMinutes;
    private Double oeePercent;          // Overall Equipment Effectiveness estimate
    private String topBottleneck;
}
