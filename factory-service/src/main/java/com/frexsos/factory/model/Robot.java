package com.frexsos.factory.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "robots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Robot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String type;

    @Column(name = "payload_capacity_kg")
    private Double payloadCapacityKg;

    @Column(name = "reach_mm")
    private Double reachMm;

    @Column(name = "operating_speed")
    private Double operatingSpeed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_id", nullable = false)
    private Factory factory;
}
