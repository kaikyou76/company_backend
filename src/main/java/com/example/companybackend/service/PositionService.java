package com.example.companybackend.service;

import com.example.companybackend.entity.Position;
import com.example.companybackend.repository.PositionRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PositionService {

    private final PositionRepository positionRepository;

    public PositionService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    public List<Position> getAllPositions() {
        return positionRepository.findAll();
    }

    public Optional<Position> getPositionById(Integer id) {
        return positionRepository.findById(id);
    }

    public Position createPosition(Position position) {
        position.setCreatedAt(OffsetDateTime.now());
        position.setUpdatedAt(OffsetDateTime.now());
        return positionRepository.save(position);
    }

    public Position updatePosition(Integer id, Position positionDetails) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Position not found with id: " + id));

        position.setName(positionDetails.getName());
        position.setLevel(positionDetails.getLevel());
        // created_atは変更しない
        position.setUpdatedAt(OffsetDateTime.now());

        return positionRepository.save(position);
    }

    public void deletePosition(Integer id) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Position not found with id: " + id));
        
        positionRepository.delete(position);
    }

    public boolean existsByName(String name) {
        return positionRepository.existsByName(name);
    }
}