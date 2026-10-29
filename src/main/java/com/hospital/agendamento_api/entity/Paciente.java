package com.hospital.agendamento_api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pacientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_responsavel_id", nullable = false)
    private Usuario usuarioResponsavel;

    // Opcionais na tabela quando parentesco = 'TITULAR'
    @Column(length = 150)
    private String nome;

    @Column(length = 11)
    private String cpf;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(nullable = false, length = 50)
    private String parentesco = "TITULAR";

    @Column(nullable = false, length = 80)
    private String convenio;

    @Column(name = "numero_carteirinha", length = 50)
    private String numeroCarteirinha;

    @Column(name = "tipo_sanguineo", length = 3)
    private String tipoSanguineo;

    @Column(columnDefinition = "TEXT")
    private String alergias;
}