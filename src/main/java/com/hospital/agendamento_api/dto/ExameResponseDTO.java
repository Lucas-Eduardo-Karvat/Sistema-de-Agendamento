package com.hospital.agendamento_api.dto;

import java.util.UUID;

public record ExameResponseDTO(
    UUID publicId,
    String nome,
    String descricao
) {}