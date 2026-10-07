package com.hospital.agendamento_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record UsuarioCadastroRequestDTO(
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 150, message = "Nome não pode passar de 150 caracteres")
    String nome,

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 números")
    String cpf,

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    String email,

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
    String senha,

    @NotBlank(message = "Telefone é obrigatório")
    String telefone,

    @NotNull(message = "Data de nascimento é obrigatória")
    @Past(message = "Data de nascimento deve ser no passado")
    LocalDate dataNascimento,

    @NotNull(message = "Endereço é obrigatório")
    @Valid
    EnderecoRequestDTO endereco
) {}