package com.frexsos.backend.service;

import com.frexsos.backend.dto.FactoryDTO;
import com.frexsos.backend.model.Factory;
import com.frexsos.backend.model.ProductionProcess;
import com.frexsos.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FactoryService {

    private final FactoryRepository factoryRepository;
    private final MachineRepository machineRepository;
    private final RobotRepository robotRepository;
    private final ProductionProcessRepository processRepository;
    private final ProcessStepRepository stepRepository;
    private final SimulationRepository simulationRepository;
    private final OperationalPerformanceLogRepository logRepository;

    @Transactional(readOnly = true)
    public List<FactoryDTO> getAllFactories() {
        return factoryRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FactoryDTO getFactoryById(Long id) {
        Factory factory = factoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Factory not found with id: " + id));
        return convertToDTO(factory);
    }

    @Transactional
    public FactoryDTO createFactory(FactoryDTO dto) {
        Factory factory = Factory.builder()
                .name(dto.getName())
                .location(dto.getLocation())
                .build();
        Factory savedFactory = factoryRepository.save(factory);
        return convertToDTO(savedFactory);
    }

    @Transactional
    public FactoryDTO updateFactory(Long id, FactoryDTO dto) {
        Factory factory = factoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Factory not found with id: " + id));
        factory.setName(dto.getName());
        factory.setLocation(dto.getLocation());
        Factory updatedFactory = factoryRepository.save(factory);
        return convertToDTO(updatedFactory);
    }

    @Transactional
    public void deleteFactory(Long id) {
        if (!factoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Factory not found with id: " + id);
        }
        // Clean up process steps for all processes in factory
        List<ProductionProcess> processes = processRepository.findByFactoryId(id);
        for (ProductionProcess p : processes) {
            stepRepository.deleteByProcessId(p.getId());
            logRepository.deleteByProcessId(p.getId());
        }
        processRepository.deleteByFactoryId(id);
        simulationRepository.deleteByFactoryId(id);
        logRepository.deleteByFactoryId(id);
        machineRepository.deleteByFactoryId(id);
        robotRepository.deleteByFactoryId(id);
        factoryRepository.deleteById(id);
    }

    private FactoryDTO convertToDTO(Factory factory) {
        return FactoryDTO.builder()
                .id(factory.getId())
                .name(factory.getName())
                .location(factory.getLocation())
                .createdDate(factory.getCreatedDate())
                .build();
    }
}
