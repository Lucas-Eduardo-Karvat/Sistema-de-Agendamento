package com.hospital.agendamento_api.service;

import com.hospital.agendamento_api.dto.AgendamentoResponseDTO;
import com.hospital.agendamento_api.dto.CancelarAgendamentoRequestDTO;
import com.hospital.agendamento_api.dto.ConfirmarAgendamentoRequestDTO;
import com.hospital.agendamento_api.entity.Agendamento;
import com.hospital.agendamento_api.entity.Medico;
import com.hospital.agendamento_api.entity.Usuario;
import com.hospital.agendamento_api.exception.BusinessException;
import com.hospital.agendamento_api.exception.ResourceNotFoundException;
import com.hospital.agendamento_api.repository.AgendamentoRepository;
import com.hospital.agendamento_api.repository.MedicoRepository;
import com.hospital.agendamento_api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RecepcaoAgendamentoService {

    // Status permitidos pela constraint chk_agendamentos_status do banco
    private static final String PENDENTE = "PENDENTE";
    private static final String CONFIRMADO = "CONFIRMADO";
    private static final String CANCELADO = "CANCELADO";
    private static final Set<String> STATUS_VALIDOS =
            Set.of("PENDENTE", "CONFIRMADO", "RECUSADO", "CONCLUIDO", "CANCELADO");

    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedicoRepository medicoRepository;

    public RecepcaoAgendamentoService(AgendamentoRepository agendamentoRepository,
                                      UsuarioRepository usuarioRepository,
                                      MedicoRepository medicoRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.medicoRepository = medicoRepository;
    }

    /** Lista os agendamentos. Sem filtro traz todos; com filtro, só o status pedido (ex.: PENDENTE). */
    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listar(String status) {
        List<Agendamento> agendamentos;

        if (status == null || status.isBlank()) {
            agendamentos = agendamentoRepository.listarTodos();
        } else {
            String statusNormalizado = status.trim().toUpperCase();
            if (!STATUS_VALIDOS.contains(statusNormalizado)) {
                throw new BusinessException("Status inválido. Use: " + String.join(", ", STATUS_VALIDOS));
            }
            agendamentos = agendamentoRepository.listarPorStatus(statusNormalizado);
        }

        return agendamentos.stream().map(this::mapearParaDTO).toList();
    }

    /**
     * Confirma um agendamento PENDENTE: a recepção escolhe uma das 3 datas sugeridas pelo paciente
     * e, opcionalmente, atribui um médico.
     */
    @Transactional
    public AgendamentoResponseDTO confirmar(UUID agendamentoPublicId,
                                            ConfirmarAgendamentoRequestDTO dto,
                                            String emailRecepcionista) {
        Usuario recepcionista = buscarUsuario(emailRecepcionista);
        Agendamento agendamento = buscarAgendamento(agendamentoPublicId);

        if (!PENDENTE.equals(agendamento.getStatus())) {
            throw new BusinessException(
                    "Só é possível confirmar agendamentos PENDENTES. Status atual: " + agendamento.getStatus());
        }

        OffsetDateTime dataEscolhida = switch (dto.opcaoEscolhida()) {
            case 1 -> agendamento.getDataOpcao1();
            case 2 -> agendamento.getDataOpcao2();
            default -> agendamento.getDataOpcao3();
        };

        // Não faz sentido confirmar uma data que já passou enquanto o pedido esperava na fila
        if (dataEscolhida.isBefore(OffsetDateTime.now())) {
            throw new BusinessException(
                    "A data escolhida já passou. Escolha outra opção ou cancele o agendamento.");
        }

        if (dto.medicoPublicId() != null) {
            Medico medico = medicoRepository.findByUsuarioPublicId(dto.medicoPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Médico não encontrado"));

            boolean horarioOcupado = agendamentoRepository.existsByMedicoIdAndDataConfirmadaAndStatus(
                    medico.getId(), dataEscolhida, CONFIRMADO);
            if (horarioOcupado) {
                throw new BusinessException("O médico já possui um exame confirmado neste horário.");
            }
            agendamento.setMedico(medico);
        }

        agendamento.setStatus(CONFIRMADO);
        agendamento.setDataConfirmada(dataEscolhida);
        agendamento.setAtendente(recepcionista);
        agendamento.setAlteradoPor(recepcionista);
        agendamento.setAlteradoEm(OffsetDateTime.now());
        if (dto.observacao() != null && !dto.observacao().isBlank()) {
            agendamento.setObservacao(dto.observacao());
        }

        return mapearParaDTO(agendamentoRepository.save(agendamento));
    }

    /** Cancela um agendamento PENDENTE ou CONFIRMADO, registrando o motivo e quem cancelou. */
    @Transactional
    public AgendamentoResponseDTO cancelar(UUID agendamentoPublicId,
                                           CancelarAgendamentoRequestDTO dto,
                                           String emailRecepcionista) {
        Usuario recepcionista = buscarUsuario(emailRecepcionista);
        Agendamento agendamento = buscarAgendamento(agendamentoPublicId);

        String status = agendamento.getStatus();
        if (!PENDENTE.equals(status) && !CONFIRMADO.equals(status)) {
            throw new BusinessException(
                    "Só é possível cancelar agendamentos PENDENTES ou CONFIRMADOS. Status atual: " + status);
        }

        agendamento.setStatus(CANCELADO);
        agendamento.setObservacao(dto.motivo());
        agendamento.setAtendente(recepcionista);
        agendamento.setAlteradoPor(recepcionista);
        agendamento.setAlteradoEm(OffsetDateTime.now());

        return mapearParaDTO(agendamentoRepository.save(agendamento));
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private Agendamento buscarAgendamento(UUID publicId) {
        return agendamentoRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));
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
