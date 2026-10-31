package com.hospital.agendamento_api.dto;

/** `token` só vem preenchido quando o e-mail mudou (o e-mail é o "subject" do JWT, então o antigo deixa de valer). */
public record PerfilAtualizadoResponseDTO(
    UsuarioResponseDTO usuario,
    String token
) {}
