package com.hospital.agendamento_api.service;

import com.hospital.agendamento_api.dto.ExameAdminResponseDTO;
import com.hospital.agendamento_api.dto.ExameCadastroRequestDTO;
import com.hospital.agendamento_api.dto.ExameResponseDTO;
import com.hospital.agendamento_api.dto.ExameUpdateRequestDTO;
import com.hospital.agendamento_api.entity.Exame;
import com.hospital.agendamento_api.exception.BusinessException;
import com.hospital.agendamento_api.exception.ResourceNotFoundException;
import com.hospital.agendamento_api.repository.AgendamentoRepository;
import com.hospital.agendamento_api.repository.ExameRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ExameService {

    private final ExameRepository exameRepository;
    private final AgendamentoRepository agendamentoRepository;

    public ExameService(ExameRepository exameRepository, AgendamentoRepository agendamentoRepository) {
        this.exameRepository = exameRepository;
        this.agendamentoRepository = agendamentoRepository;
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

    // ------------------------------------------------------------------
    // Painel administrativo (rotas /api/admin/exames, protegidas por ROLE_ADMIN no SecurityConfig)
    // ------------------------------------------------------------------

    /** Todos os exames, ativos e inativos (a listagem dos pacientes continua sendo listarExamesAtivos). */
    @Transactional(readOnly = true)
    public List<ExameAdminResponseDTO> listarTodosAdmin() {
        return exameRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"))
                .stream()
                .map(this::paraAdminDTO)
                .toList();
    }

    @Transactional
    public ExameAdminResponseDTO cadastrarAdmin(ExameCadastroRequestDTO dto) {
        // Reaproveita a validação de nome duplicado e a criação (exame novo nasce ativo)
        ExameResponseDTO criado = cadastrar(dto);
        return new ExameAdminResponseDTO(criado.publicId(), criado.nome(), criado.descricao(), true);
    }

    @Transactional
    public ExameAdminResponseDTO atualizar(UUID publicId, ExameUpdateRequestDTO dto) {
        Exame exame = buscarEntidade(publicId);
        String nome = dto.nome().trim();

        if (exameRepository.existsByNomeIgnoreCaseAndIdNot(nome, exame.getId())) {
            throw new BusinessException("Já existe outro exame cadastrado com este nome.");
        }

        exame.setNome(nome);
        exame.setDescricao(dto.descricao());
        return paraAdminDTO(exameRepository.save(exame));
    }

    @Transactional
    public ExameAdminResponseDTO alterarStatus(UUID publicId, boolean ativo) {
        Exame exame = buscarEntidade(publicId);
        exame.setAtivo(ativo);
        return paraAdminDTO(exameRepository.save(exame));
    }

    @Transactional
    public void excluir(UUID publicId) {
        Exame exame = buscarEntidade(publicId);

        if (agendamentoRepository.existsByExameId(exame.getId())) {
            throw new BusinessException(
                    "Não é possível excluir um exame que possui histórico de agendamentos. Desative-o em seu lugar.");
        }

        exameRepository.delete(exame);
    }

    private Exame buscarEntidade(UUID publicId) {
        return exameRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Exame não encontrado com o ID informado."));
    }

    private ExameAdminResponseDTO paraAdminDTO(Exame e) {
        return new ExameAdminResponseDTO(e.getPublicId(), e.getNome(), e.getDescricao(), e.getAtivo());
    }
}
