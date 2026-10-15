package com.frexsos.backend.controller;

import com.frexsos.backend.dto.FactoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequestMapping("/api/factories")
@RequiredArgsConstructor
public class FactoryController {

    private final RestTemplate restTemplate;

    @Value("${factory.service.url:http://localhost:8083}")
    private String factoryServiceUrl;

    @GetMapping
    public ResponseEntity<List<FactoryDTO>> getAllFactories() {
        String url = factoryServiceUrl + "/api/factories";
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<FactoryDTO>>() {}
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<FactoryDTO> getFactoryById(@PathVariable Long id) {
        String url = factoryServiceUrl + "/api/factories/" + id;
        return restTemplate.exchange(url, HttpMethod.GET, null, FactoryDTO.class);
    }

    @PostMapping
    public ResponseEntity<FactoryDTO> createFactory(@RequestBody FactoryDTO dto) {
        String url = factoryServiceUrl + "/api/factories";
        return restTemplate.postForEntity(url, dto, FactoryDTO.class);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FactoryDTO> updateFactory(@PathVariable Long id, @RequestBody FactoryDTO dto) {
        String url = factoryServiceUrl + "/api/factories/" + id;
        return restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(dto), FactoryDTO.class);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFactory(@PathVariable Long id) {
        String url = factoryServiceUrl + "/api/factories/" + id;
        restTemplate.delete(url);
        return ResponseEntity.noContent().build();
    }
}
