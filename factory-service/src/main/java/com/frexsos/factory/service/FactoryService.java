package com.frexsos.factory.service;

import com.frexsos.factory.dto.FactoryDTO;
import com.frexsos.factory.model.Factory;
import com.frexsos.factory.repository.*;
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

    public List<FactoryDTO> getAllFactories() {
        return factoryRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public FactoryDTO getFactory(Long id) {
        Factory factory = factoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factory not found with id: " + id));
        return toDTO(factory);
    }

    public FactoryDTO createFactory(FactoryDTO dto) {
        Factory factory = Factory.builder()
                .name(dto.getName())
                .location(dto.getLocation())
                .build();
        return toDTO(factoryRepository.save(factory));
    }

    public FactoryDTO updateFactory(Long id, FactoryDTO dto) {
        Factory factory = factoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factory not found with id: " + id));
        factory.setName(dto.getName());
        factory.setLocation(dto.getLocation());
        return toDTO(factoryRepository.save(factory));
    }

    @Transactional
    public void deleteFactory(Long id) {
        Factory factory = factoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factory not found with id: " + id));
        machineRepository.deleteAllByFactoryId(id);
        robotRepository.deleteAllByFactoryId(id);
        processRepository.deleteAllByFactoryId(id);
        factoryRepository.delete(factory);
    }

    private FactoryDTO toDTO(Factory f) {
        return FactoryDTO.builder()
                .id(f.getId())
                .name(f.getName())
                .location(f.getLocation())
                .createdDate(f.getCreatedDate() != null ? f.getCreatedDate().toString() : null)
                .build();
    }
}
