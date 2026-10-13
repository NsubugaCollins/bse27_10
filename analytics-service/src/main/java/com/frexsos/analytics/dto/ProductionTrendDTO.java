package com.frexsos.analytics.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Single production run entry for trend charts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionTrendDTO {
    private Long id;
    private Long factoryId;
    private String runName;
    private LocalDateTime runDate;
    private Integer itemCount;
    private Double totalDurationMinutes;
    private Double totalEnergyKWh;
    private Double avgCycleTimeMinutes;
    private String bottleneck;
}
