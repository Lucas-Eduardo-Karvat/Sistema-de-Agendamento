package com.hospital.agendamento_api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UsuarioResponseDTO(
    UUID publicId,
    String nome,
    String cpf,
    String email,
    String telefone,
    LocalDate dataNascimento,
    String cargo,
    Boolean ativo,
    OffsetDateTime dataCriacao,
    EnderecoResponseDTO endereco
) {}