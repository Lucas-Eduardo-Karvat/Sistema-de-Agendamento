package com.hospital.agendamento_api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AgendamentoResponseDTO(
    UUID publicId,
    UUID pacientePublicId,
    UUID solicitantePublicId,
    UUID examePublicId,
    String status,
    OffsetDateTime dataOpcao1,
    OffsetDateTime dataOpcao2,
    OffsetDateTime dataOpcao3,
    OffsetDateTime dataConfirmada,
    String observacao,
    OffsetDateTime dataCriacao
) {}