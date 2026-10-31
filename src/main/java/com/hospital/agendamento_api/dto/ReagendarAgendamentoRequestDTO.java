package com.hospital.agendamento_api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/** Novas datas para um exame já solicitado. Exame e paciente são os do agendamento original. */
public record ReagendarAgendamentoRequestDTO(
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
