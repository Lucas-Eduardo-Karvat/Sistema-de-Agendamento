package com.hospital.agendamento_api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Paciente como a recepção enxerga. Telefone, e-mail e endereço são do usuário responsável
 * (no caso de dependentes, é o contato de quem cuida do paciente).
 */
public record PacienteRecepcaoResponseDTO(
    UUID publicId,
    String nome,
    String cpf,
    LocalDate dataNascimento,
    String parentesco,
    String convenio,
    String telefone,
    String email,
    String responsavelNome,
    EnderecoResponseDTO endereco,
    OffsetDateTime ultimaConsulta,
    OffsetDateTime proximoExame,
    int totalAgendamentos
) {}
