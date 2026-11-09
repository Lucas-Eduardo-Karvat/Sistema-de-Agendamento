package com.hospital.agendamento_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminAlterarSenhaRequestDTO(
    @NotBlank(message = "Informe a nova senha")
    @Size(min = 6, max = 72, message = "A nova senha deve ter entre 6 e 72 caracteres")
    String novaSenha
) {}
