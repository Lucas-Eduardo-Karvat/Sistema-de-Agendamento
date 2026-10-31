package com.hospital.agendamento_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoRequestDTO(
    @NotBlank(message = "CEP é obrigatório")
    @Pattern(regexp = "\\d{8}", message = "CEP deve conter exatamente 8 números")
    String cep,

    @NotBlank(message = "Logradouro é obrigatório")
    String logradouro,

    @NotBlank(message = "Número é obrigatório")
    String numero,

    String complemento,

    @NotBlank(message = "Bairro é obrigatório")
    String bairro,

    @NotBlank(message = "Cidade é obrigatória")
    String cidade,

    @NotBlank(message = "Estado é obrigatório")
    @Size(min = 2, max = 2, message = "UF deve ter exatamente 2 letras")
    String estado
) {}