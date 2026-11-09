package com.hospital.agendamento_api.dto;

import jakarta.validation.constraints.NotBlank;

public record AlterarCargoRequestDTO(
    @NotBlank(message = "Informe o cargo (ADMIN, RECEPCIONISTA ou PACIENTE)")
    String nomeCargo
) {}
