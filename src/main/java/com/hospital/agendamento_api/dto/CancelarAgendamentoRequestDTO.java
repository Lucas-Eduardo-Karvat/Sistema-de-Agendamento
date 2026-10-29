package com.hospital.agendamento_api.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelarAgendamentoRequestDTO(
    @NotBlank(message = "O motivo do cancelamento é obrigatório")
    String motivo
) {}
