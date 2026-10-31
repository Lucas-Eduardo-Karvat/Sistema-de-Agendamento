package com.hospital.agendamento_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Dados que o próprio usuário pode alterar no perfil (o CPF não muda). */
public record PerfilUpdateRequestDTO(
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 150, message = "Nome não pode passar de 150 caracteres")
    String nome,

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    String email,

    @NotBlank(message = "Telefone é obrigatório")
    String telefone,

    @NotNull(message = "Data de nascimento é obrigatória")
    @Past(message = "Data de nascimento deve ser no passado")
    LocalDate dataNascimento,

    @NotNull(message = "Endereço é obrigatório")
    @Valid
    EnderecoRequestDTO endereco
) {}
