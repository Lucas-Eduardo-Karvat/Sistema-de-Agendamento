package com.hospital.agendamento_api.service;

import com.hospital.agendamento_api.dto.AgendamentoRequestDTO;
import com.hospital.agendamento_api.dto.AgendamentoResponseDTO;
import com.hospital.agendamento_api.entity.Agendamento;
import com.hospital.agendamento_api.entity.Exame;
import com.hospital.agendamento_api.entity.Paciente;
import com.hospital.agendamento_api.entity.Usuario;
import com.hospital.agendamento_api.exception.ResourceNotFoundException;
import com.hospital.agendamento_api.repository.AgendamentoRepository;
import com.hospital.agendamento_api.repository.ExameRepository;
import com.hospital.agendamento_api.repository.PacienteRepository;
import com.hospital.agendamento_api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ExameRepository exameRepository;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              PacienteRepository pacienteRepository,
                              UsuarioRepository usuarioRepository,
                              ExameRepository exameRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.exameRepository = exameRepository;
    }

    @Transactional
    public AgendamentoResponseDTO criarAgendamento(AgendamentoRequestDTO dto, String emailSolicitante) {
        // Busca o usuário logado via e-mail retornado pelo JWT
        Usuario solicitante = usuarioRepository.findByEmail(emailSolicitante)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitante não encontrado para o e-mail informado"));

        // Se o pacientePublicId não for enviado no JSON, usa o paciente TITULAR do solicitante
        Paciente paciente;
        if (dto.pacientePublicId() != null) {
            paciente = pacienteRepository.findByPublicId(dto.pacientePublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente informado não encontrado"));
        } else {
            paciente = pacienteRepository.findByUsuarioResponsavelIdAndParentesco(solicitante.getId(), "TITULAR")
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente titular não encontrado para este usuário"));
        }

        Exame exame = exameRepository.findByPublicId(dto.examePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Exame não encontrado"));

        Agendamento agendamento = new Agendamento();
        agendamento.setSolicitante(solicitante);
        agendamento.setPaciente(paciente);
        agendamento.setExame(exame);
        agendamento.setStatus("PENDENTE");
        agendamento.setDataOpcao1(dto.dataOpcao1());
        agendamento.setDataOpcao2(dto.dataOpcao2());
        agendamento.setDataOpcao3(dto.dataOpcao3());
        agendamento.setObservacao(dto.observacao());

        Agendamento salvo = agendamentoRepository.save(agendamento);

        return mapearParaDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listarPorSolicitante(String emailSolicitante) {
        Usuario solicitante = usuarioRepository.findByEmail(emailSolicitante)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitante não encontrado"));

        return agendamentoRepository.findBySolicitantePublicId(solicitante.getPublicId())
                .stream()
                .map(this::mapearParaDTO)
                .toList();
    }

    private AgendamentoResponseDTO mapearParaDTO(Agendamento a) {
        return new AgendamentoResponseDTO(
                a.getPublicId(),
                a.getPaciente().getPublicId(),
                a.getSolicitante().getPublicId(),
                a.getExame().getPublicId(),
                a.getStatus(),
                a.getDataOpcao1(),
                a.getDataOpcao2(),
                a.getDataOpcao3(),
                a.getDataConfirmada(),
                a.getObservacao(),
                a.getDataCriacao()
        );
    }
}