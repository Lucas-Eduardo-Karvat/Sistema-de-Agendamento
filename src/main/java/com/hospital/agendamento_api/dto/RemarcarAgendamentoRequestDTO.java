package com.hospital.agendamento_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record RemarcarAgendamentoRequestDTO(
    @NotNull(message = "Informe a nova data e o horário")
    OffsetDateTime dataAgendada,

    // Opcional: se vier vazio, mantém o consultório atual (obrigatório só se ainda não houver um)
    @Size(max = 100, message = "O consultório não pode passar de 100 caracteres")
    String consultorio,

    // Opcional: explicação mostrada ao paciente
    String motivo
) {}
