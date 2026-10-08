package com.hospital.agendamento_api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AgendamentoRequestDTO(
    @NotNull(message = "O ID do paciente é obrigatório")
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