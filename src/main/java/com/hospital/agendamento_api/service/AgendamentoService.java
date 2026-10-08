package com.hospital.agendamento_api.service;

import com.hospital.agendamento_api.dto.AgendamentoRequestDTO;
import com.hospital.agendamento_api.dto.AgendamentoResponseDTO;
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
                if ("REALIZADO".equalsIgnoreCase(agendamento.getStatus())) {
                        throw new BusinessException("Não é possível cancelar um agendamento já realizado.");
                }

                agendamento.setStatus("CANCELADO");
                Agendamento atualizado = agendamentoRepository.save(agendamento);

                return mapearParaDTO(atualizado);
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
                                a.getDataCriacao());
        }
}