package com.hospital.agendamento_api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConfirmarAgendamentoRequestDTO(
    @NotNull(message = "Informe qual das 3 opções de data foi escolhida (1, 2 ou 3)")
    @Min(value = 1, message = "A opção deve ser 1, 2 ou 3")
    @Max(value = 3, message = "A opção deve ser 1, 2 ou 3")
    Integer opcaoEscolhida,

    // Opcional: médico responsável pelo exame
    UUID medicoPublicId,

    String observacao
) {}
