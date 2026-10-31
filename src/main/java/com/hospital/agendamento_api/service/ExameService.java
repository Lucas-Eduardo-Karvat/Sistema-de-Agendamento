package com.hospital.agendamento_api.service;

import com.hospital.agendamento_api.dto.ExameCadastroRequestDTO;
import com.hospital.agendamento_api.dto.ExameResponseDTO;
import com.hospital.agendamento_api.entity.Exame;
import com.hospital.agendamento_api.exception.BusinessException;
import com.hospital.agendamento_api.repository.ExameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public ExameResponseDTO cadastrar(ExameCadastroRequestDTO dto) {
        String nome = dto.nome().trim();

        if (exameRepository.existsByNomeIgnoreCase(nome)) {
            throw new BusinessException("Já existe um exame cadastrado com este nome.");
        }

        Exame exame = new Exame();
        exame.setNome(nome);
        exame.setDescricao(dto.descricao());
        exame.setAtivo(true);

        Exame salvo = exameRepository.save(exame);

        return new ExameResponseDTO(salvo.getPublicId(), salvo.getNome(), salvo.getDescricao());
    }
}
