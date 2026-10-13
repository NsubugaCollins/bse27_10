package com.frexsos.factory.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "machines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String type;

    @Column(name = "power_consumption_kw")
    private Double powerConsumptionKw;

    @Column(name = "maintenance_interval_hours")
    private Integer maintenanceIntervalHours;

    @Column(name = "operating_speed")
    private Double operatingSpeed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_id", nullable = false)
    private Factory factory;
}
