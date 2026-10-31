package com.hospital.agendamento_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExameCadastroRequestDTO(
    @NotBlank(message = "O nome do exame é obrigatório")
    @Size(max = 100, message = "O nome não pode passar de 100 caracteres")
    String nome,

    @Size(max = 1000, message = "A descrição não pode passar de 1000 caracteres")
    String descricao
) {}
