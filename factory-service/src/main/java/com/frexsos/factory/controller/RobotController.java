package com.frexsos.factory.controller;

import com.frexsos.factory.dto.RobotDTO;
import com.frexsos.factory.service.RobotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RobotController {

    private final RobotService robotService;

    @GetMapping("/api/factories/{factoryId}/robots")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER','MANAGER','TECHNICIAN')")
    public ResponseEntity<List<RobotDTO>> getRobots(@PathVariable Long factoryId) {
        return ResponseEntity.ok(robotService.getRobotsByFactory(factoryId));
    }

    @PostMapping("/api/factories/{factoryId}/robots")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER','MANAGER')")
    public ResponseEntity<RobotDTO> createRobot(@PathVariable Long factoryId, @RequestBody RobotDTO dto) {
        return ResponseEntity.ok(robotService.createRobot(factoryId, dto));
    }

    @PutMapping("/api/robots/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER','MANAGER')")
    public ResponseEntity<RobotDTO> updateRobot(@PathVariable Long id, @RequestBody RobotDTO dto) {
        return ResponseEntity.ok(robotService.updateRobot(id, dto));
    }

    @DeleteMapping("/api/robots/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public ResponseEntity<Void> deleteRobot(@PathVariable Long id) {
        robotService.deleteRobot(id);
        return ResponseEntity.noContent().build();
    }
}
