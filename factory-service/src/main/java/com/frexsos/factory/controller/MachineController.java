package com.frexsos.factory.controller;

import com.frexsos.factory.dto.MachineDTO;
import com.frexsos.factory.service.MachineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MachineController {

    private final MachineService machineService;

    @GetMapping("/api/factories/{factoryId}/machines")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER','MANAGER','TECHNICIAN')")
    public ResponseEntity<List<MachineDTO>> getMachines(@PathVariable Long factoryId) {
        return ResponseEntity.ok(machineService.getMachinesByFactory(factoryId));
    }

    @PostMapping("/api/factories/{factoryId}/machines")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER','MANAGER')")
    public ResponseEntity<MachineDTO> createMachine(@PathVariable Long factoryId, @RequestBody MachineDTO dto) {
        return ResponseEntity.ok(machineService.createMachine(factoryId, dto));
    }

    @PutMapping("/api/machines/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER','MANAGER')")
    public ResponseEntity<MachineDTO> updateMachine(@PathVariable Long id, @RequestBody MachineDTO dto) {
        return ResponseEntity.ok(machineService.updateMachine(id, dto));
    }

    @DeleteMapping("/api/machines/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public ResponseEntity<Void> deleteMachine(@PathVariable Long id) {
        machineService.deleteMachine(id);
        return ResponseEntity.noContent().build();
    }
}
