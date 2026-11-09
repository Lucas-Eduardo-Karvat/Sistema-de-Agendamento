package com.hospital.agendamento_api.dto;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusExameRequestDTO(
    @NotNull(message = "Informe o status (ativo: true ou false)")
    Boolean ativo
) {}
