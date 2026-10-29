package com.hospital.agendamento_api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "medicos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Medico {

    @Id
    @Column(name = "usuario_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 20)
    private String crm;

    @Column(name = "uf_crm", nullable = false, length = 2)
    private String ufCrm;

    @Column(nullable = false, length = 100)
    private String especialidade;
}