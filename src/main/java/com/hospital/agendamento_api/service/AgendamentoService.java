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
import java.util.UUID;

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
    public AgendamentoResponseDTO criarAgendamento(AgendamentoRequestDTO dto, UUID solicitantePublicId) {
        // 1. Busca o usuário logado (solicitante)
        Usuario solicitante = usuarioRepository.findByPublicId(solicitantePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitante não encontrado"));

        // 2. Busca o paciente (se enviado UUID usa o informado, senão busca o TITULAR do solicitante)
        Paciente paciente;
        if (dto.pacientePublicId() != null) {
            paciente = pacienteRepository.findByPublicId(dto.pacientePublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente informado não encontrado"));
        } else {
            paciente = pacienteRepository.findByUsuarioResponsavelIdAndParentesco(solicitante.getId(), "TITULAR")
                    .orElseThrow(() -> new ResourceNotFoundException("Cadastro de paciente titular não encontrado para o usuário"));
        }

        // 3. Busca o exame
        Exame exame = exameRepository.findByPublicId(dto.examePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Exame não encontrado"));

        // 4. Cria e persiste a entidade
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
    public List<AgendamentoResponseDTO> listarPorSolicitante(UUID solicitantePublicId) {
        return agendamentoRepository.findBySolicitantePublicId(solicitantePublicId)
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