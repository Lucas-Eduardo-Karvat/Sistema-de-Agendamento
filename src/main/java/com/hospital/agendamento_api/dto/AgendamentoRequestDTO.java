package com.hospital.agendamento_api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AgendamentoRequestDTO(
    // Opcional: nulo indica que o exame é para o titular logado
    UUID pacientePublicId,

    @NotNull(message = "O ID do exame é obrigatório")
    UUID examePublicId,

    @NotNull(message = "A primeira opção de data é obrigatória")
    @Future(message = "A data deve ser futura")
    OffsetDateTime dataOpcao1,

    @NotNull(message = "A segunda opção de data é obrigatória")
    @Future(message = "A data deve ser futura")
    OffsetDateTime dataOpcao2,

    @NotNull(message = "A terceira opção de data é obrigatória")
    @Future(message = "A data deve ser futura")
    OffsetDateTime dataOpcao3,

    String observacao
) {}