package com.frexsos.factory.service;

import com.frexsos.factory.dto.RobotDTO;
import com.frexsos.factory.model.Factory;
import com.frexsos.factory.model.Robot;
import com.frexsos.factory.repository.FactoryRepository;
import com.frexsos.factory.repository.RobotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RobotService {

    private final RobotRepository robotRepository;
    private final FactoryRepository factoryRepository;

    public List<RobotDTO> getRobotsByFactory(Long factoryId) {
        return robotRepository.findByFactoryId(factoryId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public RobotDTO createRobot(Long factoryId, RobotDTO dto) {
        Factory factory = factoryRepository.findById(factoryId)
                .orElseThrow(() -> new RuntimeException("Factory not found with id: " + factoryId));
        Robot robot = Robot.builder()
                .name(dto.getName())
                .type(dto.getType())
                .payloadCapacityKg(dto.getPayloadCapacityKg())
                .reachMm(dto.getReachMm())
                .operatingSpeed(dto.getOperatingSpeed())
                .factory(factory)
                .build();
        return toDTO(robotRepository.save(robot));
    }

    public RobotDTO updateRobot(Long id, RobotDTO dto) {
        Robot robot = robotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Robot not found with id: " + id));
        robot.setName(dto.getName());
        robot.setType(dto.getType());
        robot.setPayloadCapacityKg(dto.getPayloadCapacityKg());
        robot.setReachMm(dto.getReachMm());
        robot.setOperatingSpeed(dto.getOperatingSpeed());
        return toDTO(robotRepository.save(robot));
    }

    public void deleteRobot(Long id) {
        robotRepository.deleteById(id);
    }

    private RobotDTO toDTO(Robot r) {
        return RobotDTO.builder()
                .id(r.getId())
                .name(r.getName())
                .type(r.getType())
                .payloadCapacityKg(r.getPayloadCapacityKg())
                .reachMm(r.getReachMm())
                .operatingSpeed(r.getOperatingSpeed())
                .factoryId(r.getFactory().getId())
                .build();
    }
}
