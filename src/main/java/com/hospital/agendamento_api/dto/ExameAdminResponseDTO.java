package com.hospital.agendamento_api.dto;

import java.util.UUID;

public record ExameAdminResponseDTO(
    UUID publicId,
    String nome,
    String descricao,
    Boolean ativo
) {}
