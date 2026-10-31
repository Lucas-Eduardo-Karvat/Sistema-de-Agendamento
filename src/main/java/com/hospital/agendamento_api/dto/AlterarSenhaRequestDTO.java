package com.hospital.agendamento_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequestDTO(
    @NotBlank(message = "Informe a senha atual")
    String senhaAtual,

    @NotBlank(message = "Informe a nova senha")
    @Size(min = 6, message = "A nova senha deve ter no mínimo 6 caracteres")
    String novaSenha
) {}
