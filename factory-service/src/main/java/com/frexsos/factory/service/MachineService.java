package com.frexsos.factory.service;

import com.frexsos.factory.dto.MachineDTO;
import com.frexsos.factory.model.Factory;
import com.frexsos.factory.model.Machine;
import com.frexsos.factory.repository.FactoryRepository;
import com.frexsos.factory.repository.MachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MachineService {

    private final MachineRepository machineRepository;
    private final FactoryRepository factoryRepository;

    public List<MachineDTO> getMachinesByFactory(Long factoryId) {
        return machineRepository.findByFactoryId(factoryId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public MachineDTO createMachine(Long factoryId, MachineDTO dto) {
        Factory factory = factoryRepository.findById(factoryId)
                .orElseThrow(() -> new RuntimeException("Factory not found with id: " + factoryId));
        Machine machine = Machine.builder()
                .name(dto.getName())
                .type(dto.getType())
                .powerConsumptionKw(dto.getPowerConsumptionKw())
                .maintenanceIntervalHours(dto.getMaintenanceIntervalHours())
                .operatingSpeed(dto.getOperatingSpeed())
                .factory(factory)
                .build();
        return toDTO(machineRepository.save(machine));
    }

    public MachineDTO updateMachine(Long id, MachineDTO dto) {
        Machine machine = machineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Machine not found with id: " + id));
        machine.setName(dto.getName());
        machine.setType(dto.getType());
        machine.setPowerConsumptionKw(dto.getPowerConsumptionKw());
        machine.setMaintenanceIntervalHours(dto.getMaintenanceIntervalHours());
        machine.setOperatingSpeed(dto.getOperatingSpeed());
        return toDTO(machineRepository.save(machine));
    }

    public void deleteMachine(Long id) {
        machineRepository.deleteById(id);
    }

    private MachineDTO toDTO(Machine m) {
        return MachineDTO.builder()
                .id(m.getId())
                .name(m.getName())
                .type(m.getType())
                .powerConsumptionKw(m.getPowerConsumptionKw())
                .maintenanceIntervalHours(m.getMaintenanceIntervalHours())
                .operatingSpeed(m.getOperatingSpeed())
                .factoryId(m.getFactory().getId())
                .build();
    }
}
