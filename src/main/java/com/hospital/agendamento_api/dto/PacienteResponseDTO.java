package com.hospital.agendamento_api.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PacienteResponseDTO(
    UUID publicId,
    String nome,
    String cpf,
    LocalDate dataNascimento,
    String parentesco,
    String convenio
) {}