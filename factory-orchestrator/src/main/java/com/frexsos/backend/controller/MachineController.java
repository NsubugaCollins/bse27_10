package com.frexsos.backend.controller;

import com.frexsos.backend.dto.MachineDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MachineController {

    private final RestTemplate restTemplate;

    @Value("${factory.service.url:http://localhost:8083}")
    private String factoryServiceUrl;

    @GetMapping("/factories/{factoryId}/machines")
    public ResponseEntity<List<MachineDTO>> getMachinesByFactory(@PathVariable Long factoryId) {
        String url = factoryServiceUrl + "/api/factories/" + factoryId + "/machines";
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<MachineDTO>>() {}
        );
    }

    @PostMapping("/factories/{factoryId}/machines")
    public ResponseEntity<MachineDTO> createMachine(@PathVariable Long factoryId, @RequestBody MachineDTO dto) {
        String url = factoryServiceUrl + "/api/factories/" + factoryId + "/machines";
        return restTemplate.postForEntity(url, dto, MachineDTO.class);
    }

    @PutMapping("/machines/{id}")
    public ResponseEntity<MachineDTO> updateMachine(@PathVariable Long id, @RequestBody MachineDTO dto) {
        String url = factoryServiceUrl + "/api/machines/" + id;
        return restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(dto), MachineDTO.class);
    }

    @DeleteMapping("/machines/{id}")
    public ResponseEntity<Void> deleteMachine(@PathVariable Long id) {
        String url = factoryServiceUrl + "/api/machines/" + id;
        restTemplate.delete(url);
        return ResponseEntity.noContent().build();
    }
}
