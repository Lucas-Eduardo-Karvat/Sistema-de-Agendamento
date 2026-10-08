package com.hospital.agendamento_api.service;

import com.hospital.agendamento_api.dto.ExameResponseDTO;
import com.hospital.agendamento_api.repository.ExameRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExameService {

    private final ExameRepository exameRepository;

    public ExameService(ExameRepository exameRepository) {
        this.exameRepository = exameRepository;
    }

    public List<ExameResponseDTO> listarExamesAtivos() {
        return exameRepository.findByAtivoTrue()
                .stream()
                .map(e -> new ExameResponseDTO(
                        e.getPublicId(),
                        e.getNome(),
                        e.getDescricao()
                ))
                .toList();
    }
}