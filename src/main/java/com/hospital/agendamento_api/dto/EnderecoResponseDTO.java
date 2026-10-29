package com.hospital.agendamento_api.dto;

import java.util.UUID;

public record EnderecoResponseDTO(
    UUID publicId,
    String cep,
    String logradouro,
    String numero,
    String complemento,
    String bairro,
    String cidade,
    String estado
) {}