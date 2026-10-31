package com.hospital.agendamento_api.dto;

import java.util.UUID;

public record TokenResponseDTO(
    String token,
    String tipo, // Ex: "Bearer"
    UUID usuarioPublicId,
    String nome,
    String cargo
) {}