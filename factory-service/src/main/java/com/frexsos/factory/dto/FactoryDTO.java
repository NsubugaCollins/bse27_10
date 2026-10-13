package com.frexsos.factory.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactoryDTO {
    private Long id;
    private String name;
    private String location;
    private String createdDate;
}
