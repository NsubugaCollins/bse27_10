package com.frexsos.backend.controller;

import com.frexsos.backend.dto.RobotDTO;
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
public class RobotController {

    private final RestTemplate restTemplate;

    @Value("${factory.service.url:http://localhost:8083}")
    private String factoryServiceUrl;

    @GetMapping("/factories/{factoryId}/robots")
    public ResponseEntity<List<RobotDTO>> getRobotsByFactory(@PathVariable Long factoryId) {
        String url = factoryServiceUrl + "/api/factories/" + factoryId + "/robots";
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<RobotDTO>>() {}
        );
    }

    @PostMapping("/factories/{factoryId}/robots")
    public ResponseEntity<RobotDTO> createRobot(@PathVariable Long factoryId, @RequestBody RobotDTO dto) {
        String url = factoryServiceUrl + "/api/factories/" + factoryId + "/robots";
        return restTemplate.postForEntity(url, dto, RobotDTO.class);
    }

    @PutMapping("/robots/{id}")
    public ResponseEntity<RobotDTO> updateRobot(@PathVariable Long id, @RequestBody RobotDTO dto) {
        String url = factoryServiceUrl + "/api/robots/" + id;
        return restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(dto), RobotDTO.class);
    }

    @DeleteMapping("/robots/{id}")
    public ResponseEntity<Void> deleteRobot(@PathVariable Long id) {
        String url = factoryServiceUrl + "/api/robots/" + id;
        restTemplate.delete(url);
        return ResponseEntity.noContent().build();
    }
}
