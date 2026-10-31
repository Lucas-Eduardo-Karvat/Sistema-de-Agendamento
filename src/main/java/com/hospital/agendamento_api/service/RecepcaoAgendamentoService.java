package com.hospital.agendamento_api.service;

import com.hospital.agendamento_api.dto.AgendamentoRecepcaoRequestDTO;
import com.hospital.agendamento_api.dto.AgendamentoResponseDTO;
import com.hospital.agendamento_api.dto.CancelarAgendamentoRequestDTO;
import com.hospital.agendamento_api.dto.ConfirmarAgendamentoRequestDTO;
import com.hospital.agendamento_api.dto.RemarcarAgendamentoRequestDTO;
import com.hospital.agendamento_api.entity.Agendamento;
import com.hospital.agendamento_api.entity.Exame;
import com.hospital.agendamento_api.entity.Medico;
import com.hospital.agendamento_api.entity.Paciente;
import com.hospital.agendamento_api.entity.Usuario;
import com.hospital.agendamento_api.exception.BusinessException;
import com.hospital.agendamento_api.exception.ResourceNotFoundException;
import com.hospital.agendamento_api.repository.AgendamentoRepository;
import com.hospital.agendamento_api.repository.ExameRepository;
import com.hospital.agendamento_api.repository.MedicoRepository;
import com.hospital.agendamento_api.repository.PacienteRepository;
import com.hospital.agendamento_api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RecepcaoAgendamentoService {

    // Status permitidos pela constraint chk_agendamentos_status do banco
    private static final String PENDENTE = "PENDENTE";
    private static final String CONFIRMADO = "CONFIRMADO";
    private static final String CANCELADO = "CANCELADO";
    private static final String RECUSADO = "RECUSADO";
    private static final String CONCLUIDO = "CONCLUIDO";
    private static final Set<String> STATUS_VALIDOS =
            Set.of("PENDENTE", "CONFIRMADO", "RECUSADO", "CONCLUIDO", "CANCELADO");

    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final ExameRepository exameRepository;

    public RecepcaoAgendamentoService(AgendamentoRepository agendamentoRepository,
                                      UsuarioRepository usuarioRepository,
                                      MedicoRepository medicoRepository,
                                      PacienteRepository pacienteRepository,
                                      ExameRepository exameRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.exameRepository = exameRepository;
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
        agendamento.setConsultorio(dto.consultorio().trim());
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

    /** Recusa uma solicitação PENDENTE (ex.: exame indisponível), guardando o motivo. */
    @Transactional
    public AgendamentoResponseDTO recusar(UUID agendamentoPublicId,
                                          CancelarAgendamentoRequestDTO dto,
                                          String emailRecepcionista) {
        Usuario recepcionista = buscarUsuario(emailRecepcionista);
        Agendamento agendamento = buscarAgendamento(agendamentoPublicId);

        if (!PENDENTE.equals(agendamento.getStatus())) {
            throw new BusinessException(
                    "Só é possível recusar agendamentos PENDENTES. Status atual: " + agendamento.getStatus());
        }

        agendamento.setStatus(RECUSADO);
        agendamento.setObservacao(dto.motivo());
        registrarAlteracao(agendamento, recepcionista);

        return mapearParaDTO(agendamentoRepository.save(agendamento));
    }

    /** Marca como CONCLUIDO um exame que estava CONFIRMADO (o paciente já foi atendido). */
    @Transactional
    public AgendamentoResponseDTO concluir(UUID agendamentoPublicId, String emailRecepcionista) {
        Usuario recepcionista = buscarUsuario(emailRecepcionista);
        Agendamento agendamento = buscarAgendamento(agendamentoPublicId);

        if (!CONFIRMADO.equals(agendamento.getStatus())) {
            throw new BusinessException(
                    "Só é possível concluir agendamentos CONFIRMADOS. Status atual: " + agendamento.getStatus());
        }

        agendamento.setStatus(CONCLUIDO);
        registrarAlteracao(agendamento, recepcionista);

        return mapearParaDTO(agendamentoRepository.save(agendamento));
    }

    /** A recepção agenda direto para um paciente: o agendamento já nasce CONFIRMADO. */
    @Transactional
    public AgendamentoResponseDTO criar(AgendamentoRecepcaoRequestDTO dto, String emailRecepcionista) {
        Usuario recepcionista = buscarUsuario(emailRecepcionista);

        Paciente paciente = pacienteRepository.findByPublicId(dto.pacientePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));
        if (!Boolean.TRUE.equals(paciente.getAtivo())) {
            throw new ResourceNotFoundException("Paciente não encontrado");
        }
        Exame exame = exameRepository.findByPublicId(dto.examePublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Exame não encontrado"));

        if (!dto.dataAgendada().isAfter(OffsetDateTime.now())) {
            throw new BusinessException("A data e o horário do exame devem estar no futuro.");
        }

        Agendamento agendamento = new Agendamento();
        agendamento.setPaciente(paciente);
        // O agendamento fica na conta do responsável pelo paciente, então aparece para ele em "meus agendamentos"
        agendamento.setSolicitante(paciente.getUsuarioResponsavel());
        agendamento.setExame(exame);
        agendamento.setStatus(CONFIRMADO);
        // As colunas das 3 opções são obrigatórias no banco; aqui todas repetem a data definida pela recepção
        agendamento.setDataOpcao1(dto.dataAgendada());
        agendamento.setDataOpcao2(dto.dataAgendada());
        agendamento.setDataOpcao3(dto.dataAgendada());
        agendamento.setDataConfirmada(dto.dataAgendada());
        agendamento.setConsultorio(dto.consultorio().trim());
        if (dto.observacao() != null && !dto.observacao().isBlank()) {
            agendamento.setObservacao(dto.observacao());
        }
        agendamento.setAtendente(recepcionista);
        registrarAlteracao(agendamento, recepcionista);

        return mapearParaDTO(agendamentoRepository.save(agendamento));
    }

    /**
     * Remarca um agendamento PENDENTE ou CONFIRMADO para outro dia/horário. O agendamento
     * fica CONFIRMADO na nova data, e o paciente vê a data nova (e o consultório, se mudou).
     */
    @Transactional
    public AgendamentoResponseDTO remarcar(UUID agendamentoPublicId,
                                           RemarcarAgendamentoRequestDTO dto,
                                           String emailRecepcionista) {
        Usuario recepcionista = buscarUsuario(emailRecepcionista);
        Agendamento agendamento = buscarAgendamento(agendamentoPublicId);

        String status = agendamento.getStatus();
        if (!PENDENTE.equals(status) && !CONFIRMADO.equals(status)) {
            throw new BusinessException(
                    "Só é possível remarcar agendamentos PENDENTES ou CONFIRMADOS. Status atual: " + status);
        }

        OffsetDateTime novaData = dto.dataAgendada();
        if (!novaData.isAfter(OffsetDateTime.now())) {
            throw new BusinessException("A nova data e o horário devem estar no futuro.");
        }

        String consultorio = dto.consultorio() != null && !dto.consultorio().isBlank()
                ? dto.consultorio().trim()
                : agendamento.getConsultorio();
        if (consultorio == null || consultorio.isBlank()) {
            throw new BusinessException("Informe o consultório onde o exame será realizado.");
        }

        OffsetDateTime dataAnterior = agendamento.getDataConfirmada();

        if (agendamento.getMedico() != null && !novaData.equals(dataAnterior)
                && agendamentoRepository.existsByMedicoIdAndDataConfirmadaAndStatus(
                        agendamento.getMedico().getId(), novaData, CONFIRMADO)) {
            throw new BusinessException("O médico já possui um exame confirmado neste horário.");
        }

        agendamento.setStatus(CONFIRMADO);
        agendamento.setDataConfirmada(novaData);
        agendamento.setConsultorio(consultorio);

        if (dto.motivo() != null && !dto.motivo().isBlank()) {
            agendamento.setObservacao(dto.motivo().trim());
        } else if (dataAnterior != null) {
            String anterior = dataAnterior.atZoneSameInstant(ZoneId.of("America/Sao_Paulo"))
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));
            agendamento.setObservacao("Exame remarcado. Data anterior: " + anterior + ".");
        }
        registrarAlteracao(agendamento, recepcionista);

        return mapearParaDTO(agendamentoRepository.save(agendamento));
    }

    private void registrarAlteracao(Agendamento agendamento, Usuario recepcionista) {
        agendamento.setAtendente(recepcionista);
        agendamento.setAlteradoPor(recepcionista);
        agendamento.setAlteradoEm(OffsetDateTime.now());
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
        return AgendamentoMapper.toDTO(a);
    }
}
