package com.frexsos.factory.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "process_steps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(nullable = false)
    private Double duration; // minutes

    @Column(name = "resource_type", nullable = false)
    private String resourceType; // MACHINE or ROBOT

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "resource_name")
    private String resourceName;

    @Column(name = "operating_speed")
    private Double operatingSpeed;

    @Column(name = "power_consumption")
    private Double powerConsumption; // kW

    @Column(name = "maintenance_interval")
    private Integer maintenanceInterval; // hours

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id", nullable = false)
    private ProductionProcess process;
}
