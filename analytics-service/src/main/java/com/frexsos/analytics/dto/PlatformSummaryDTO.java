package com.frexsos.analytics.dto;

import lombok.*;

/**
 * Platform-wide summary across all factories.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSummaryDTO {
    private Long totalFactories;
    private Long totalSimulations;
    private Long totalProductionRuns;
    private Long totalItemsProduced;
    private Double avgEnergyKWh;
    private Double avgCycleTimeMinutes;
    private Double platformOeePercent;
}
