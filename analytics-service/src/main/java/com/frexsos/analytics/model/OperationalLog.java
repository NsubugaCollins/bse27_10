package com.frexsos.analytics.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Read-only view of operational_performance_logs table (written by backend).
 * Analytics service only reads — never writes — this table.
 */
@Entity
@Table(name = "operational_performance_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationalLog {

    @Id
    private Long id;

    @Column(name = "factory_id")
    private Long factoryId;

    @Column(name = "process_id")
    private Long processId;

    private String name;

    @Column(name = "run_date")
    private LocalDateTime runDate;

    @Column(name = "item_count")
    private Integer itemCount;

    @Column(name = "num_lines")
    private Integer numLines;

    @Column(name = "total_duration_minutes")
    private Double totalDurationMinutes;

    @Column(name = "total_energy_kwh")
    private Double totalEnergyKWh;

    @Column(name = "avg_cycle_time_minutes")
    private Double avgCycleTimeMinutes;

    private String bottleneck;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;
}
