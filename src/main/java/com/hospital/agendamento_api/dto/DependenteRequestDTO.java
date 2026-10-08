package com.hospital.agendamento_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DependenteRequestDTO(
    @NotBlank(message = "O nome é obrigatório")
    String nome,

    String cpf, // Pode ser null se for criança sem CPF

    @NotNull(message = "A data de nascimento é obrigatória")
    LocalDate dataNascimento,

    @NotBlank(message = "O parentesco é obrigatório (ex: FILHO, MAE, CONJUGE)")
    String parentesco,

    String convenio
) {}