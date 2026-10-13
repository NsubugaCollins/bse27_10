package com.frexsos.factory.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineDTO {
    private Long id;
    private String name;
    private String type;
    private Double powerConsumptionKw;
    private Integer maintenanceIntervalHours;
    private Double operatingSpeed;
    private Long factoryId;
}
