package com.frexsos.factory.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RobotDTO {
    private Long id;
    private String name;
    private String type;
    private Double payloadCapacityKg;
    private Double reachMm;
    private Double operatingSpeed;
    private Long factoryId;
}
