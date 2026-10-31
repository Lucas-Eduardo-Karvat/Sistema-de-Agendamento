package com.hospital.agendamento_api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
    @NotBlank(message = "E-mail ou CPF é obrigatório")
    String login, // Aceita tanto e-mail quanto CPF

    @NotBlank(message = "Senha é obrigatória")
    String senha
) {}