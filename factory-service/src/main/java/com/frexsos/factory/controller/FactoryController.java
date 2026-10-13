package com.frexsos.factory.controller;

import com.frexsos.factory.dto.FactoryDTO;
import com.frexsos.factory.service.FactoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/factories")
@RequiredArgsConstructor
public class FactoryController {

    private final FactoryService factoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER','MANAGER','TECHNICIAN')")
    public ResponseEntity<List<FactoryDTO>> getAllFactories() {
        return ResponseEntity.ok(factoryService.getAllFactories());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER','MANAGER','TECHNICIAN')")
    public ResponseEntity<FactoryDTO> getFactory(@PathVariable Long id) {
        return ResponseEntity.ok(factoryService.getFactory(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER','MANAGER')")
    public ResponseEntity<FactoryDTO> createFactory(@RequestBody FactoryDTO dto) {
        return ResponseEntity.ok(factoryService.createFactory(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER','MANAGER')")
    public ResponseEntity<FactoryDTO> updateFactory(@PathVariable Long id, @RequestBody FactoryDTO dto) {
        return ResponseEntity.ok(factoryService.updateFactory(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public ResponseEntity<Void> deleteFactory(@PathVariable Long id) {
        factoryService.deleteFactory(id);
        return ResponseEntity.noContent().build();
    }
}
