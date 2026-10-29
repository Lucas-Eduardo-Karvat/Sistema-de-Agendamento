package com.hospital.agendamento_api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "agendamentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exame_id", nullable = false)
    private Exame exame;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id")
    private Medico medico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atendente_id")
    private Usuario atendente;

    @Column(nullable = false, length = 20)
    private String status = "PENDENTE";

    @Column(name = "data_opcao_1", nullable = false)
    private OffsetDateTime dataOpcao1;

    @Column(name = "data_opcao_2", nullable = false)
    private OffsetDateTime dataOpcao2;

    @Column(name = "data_opcao_3", nullable = false)
    private OffsetDateTime dataOpcao3;

    @Column(name = "data_confirmada")
    private OffsetDateTime dataConfirmada;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alterado_por")
    private Usuario alteradoPor;

    @Column(name = "alterado_em")
    private OffsetDateTime alteradoEm;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private OffsetDateTime dataCriacao = OffsetDateTime.now();
}