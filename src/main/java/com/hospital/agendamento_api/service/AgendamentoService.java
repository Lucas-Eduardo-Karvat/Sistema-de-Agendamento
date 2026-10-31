package com.hospital.agendamento_api.service;

import com.hospital.agendamento_api.dto.AgendamentoRequestDTO;
import com.hospital.agendamento_api.dto.AgendamentoResponseDTO;
import com.hospital.agendamento_api.dto.ReagendarAgendamentoRequestDTO;
import com.hospital.agendamento_api.entity.Agendamento;
import com.hospital.agendamento_api.entity.Exame;
import com.hospital.agendamento_api.entity.Paciente;
import com.hospital.agendamento_api.entity.Usuario;
import com.hospital.agendamento_api.exception.BusinessException;
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
        public AgendamentoResponseDTO criarAgendamento(AgendamentoRequestDTO dto, String emailSolicitante) {
                Usuario solicitante = usuarioRepository.findByEmail(emailSolicitante)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Solicitante não encontrado para o e-mail informado"));

                Paciente paciente;
                if (dto.pacientePublicId() != null) {
                        paciente = pacienteRepository.findByPublicId(dto.pacientePublicId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Paciente informado não encontrado"));

                        // Só pode agendar para si mesmo ou para os próprios dependentes (ainda não removidos)
                        if (!paciente.getUsuarioResponsavel().getId().equals(solicitante.getId())
                                        || !Boolean.TRUE.equals(paciente.getAtivo())) {
                                throw new ResourceNotFoundException("Paciente informado não encontrado");
                        }
                } else {
                        paciente = pacienteRepository
                                        .findByUsuarioResponsavelIdAndParentesco(solicitante.getId(), "TITULAR")
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Paciente titular não encontrado para este usuário"));
                }

                Exame exame = exameRepository.findByPublicId(dto.examePublicId())
                                .orElseThrow(() -> new ResourceNotFoundException("Exame não encontrado"));

                // TRAVA CORRETA: Bloqueia se o paciente já possui uma solicitação pendente para
                // este exame
                boolean possuiSolicitacaoPendente = agendamentoRepository.existsByPacienteIdAndExameIdAndStatus(
                                paciente.getId(),
                                exame.getId(),
                                "PENDENTE");

                if (possuiSolicitacaoPendente) {
                        throw new BusinessException(
                                        "O paciente já possui uma solicitação pendente para este exame. " +
                                                        "Aguarde a análise da recepção ou cancele o agendamento atual para solicitar novas datas.");
                }

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

        /**
         * Reagendamento feito pelo paciente: cria uma nova solicitação (PENDENTE) para o mesmo exame e o mesmo
         * paciente, com as novas datas, e apaga o agendamento antigo. Tudo na mesma transação: se algo falhar,
         * o agendamento antigo continua como estava.
         */
        @Transactional
        public AgendamentoResponseDTO reagendar(UUID agendamentoPublicId,
                        ReagendarAgendamentoRequestDTO dto,
                        String emailSolicitante) {
                Usuario solicitante = usuarioRepository.findByEmail(emailSolicitante)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Solicitante não encontrado para o e-mail informado"));
                Agendamento antigo = agendamentoRepository
                                .findByPublicIdAndSolicitantePublicId(agendamentoPublicId, solicitante.getPublicId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Agendamento não encontrado para este usuário"));
                if ("CONCLUIDO".equalsIgnoreCase(antigo.getStatus())) {
                        throw new BusinessException(
                                        "Exames já realizados não podem ser reagendados. Faça uma nova solicitação.");
                }
                Paciente paciente = antigo.getPaciente();
                if (!Boolean.TRUE.equals(paciente.getAtivo())) {
                        throw new ResourceNotFoundException("Agendamento não encontrado para este usuário");
                }
                Exame exame = antigo.getExame();

                // Apaga o antigo antes de checar pendências: se ele era o pendente, não conta contra o novo
                agendamentoRepository.delete(antigo);
                agendamentoRepository.flush();

                if (agendamentoRepository.existsByPacienteIdAndExameIdAndStatus(paciente.getId(), exame.getId(),
                                "PENDENTE")) {
                        throw new BusinessException(
                                        "O paciente já possui outra solicitação pendente para este exame. "
                                                        + "Aguarde a análise da recepção ou cancele-a antes de reagendar.");
                }

                Agendamento novo = new Agendamento();
                novo.setSolicitante(solicitante);
                novo.setPaciente(paciente);
                novo.setExame(exame);
                novo.setStatus("PENDENTE");
                novo.setDataOpcao1(dto.dataOpcao1());
                novo.setDataOpcao2(dto.dataOpcao2());
                novo.setDataOpcao3(dto.dataOpcao3());
                novo.setObservacao(dto.observacao());
                return mapearParaDTO(agendamentoRepository.save(novo));
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

        @Transactional(readOnly = true)
        public AgendamentoResponseDTO buscarPorPublicId(UUID agendamentoPublicId, String emailSolicitante) {
                Usuario solicitante = usuarioRepository.findByEmail(emailSolicitante)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Solicitante não encontrado para o e-mail informado"));

                Agendamento agendamento = agendamentoRepository
                                .findByPublicIdAndSolicitantePublicId(agendamentoPublicId, solicitante.getPublicId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Agendamento não encontrado para este usuário"));

                return mapearParaDTO(agendamento);
        }

        @Transactional
        public AgendamentoResponseDTO cancelarPeloPaciente(UUID agendamentoPublicId, String emailSolicitante) {
                Usuario solicitante = usuarioRepository.findByEmail(emailSolicitante)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Solicitante não encontrado para o e-mail informado"));

                Agendamento agendamento = agendamentoRepository
                                .findByPublicIdAndSolicitantePublicId(agendamentoPublicId, solicitante.getPublicId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Agendamento não encontrado para este usuário"));

                if ("CANCELADO".equalsIgnoreCase(agendamento.getStatus())) {
                        throw new BusinessException("Este agendamento já está cancelado.");
                }
                if ("CONCLUIDO".equalsIgnoreCase(agendamento.getStatus())) {
                        throw new BusinessException("Não é possível cancelar um agendamento já realizado.");
                }
                if ("RECUSADO".equalsIgnoreCase(agendamento.getStatus())) {
                        throw new BusinessException("Este agendamento foi recusado e não pode ser cancelado.");
                }

                agendamento.setStatus("CANCELADO");
                Agendamento atualizado = agendamentoRepository.save(agendamento);

                return mapearParaDTO(atualizado);
        }

        private AgendamentoResponseDTO mapearParaDTO(Agendamento a) {
        return AgendamentoMapper.toDTO(a);
    }
}
