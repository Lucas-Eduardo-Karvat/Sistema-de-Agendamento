package com.hospital.agendamento_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Agendamento criado direto pela recepção: já nasce CONFIRMADO, com data e consultório definidos. */
public record AgendamentoRecepcaoRequestDTO(
    @NotNull(message = "Informe o paciente")
    UUID pacientePublicId,

    @NotNull(message = "Informe o exame")
    UUID examePublicId,

    @NotNull(message = "Informe a data e o horário")
    OffsetDateTime dataAgendada,

    @NotBlank(message = "Informe o consultório onde o exame será realizado")
    @Size(max = 100, message = "O consultório não pode passar de 100 caracteres")
    String consultorio,

    String observacao
) {}
