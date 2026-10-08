package com.hospital.agendamento_api.repository;

import com.hospital.agendamento_api.entity.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    @Query("SELECT a FROM Agendamento a WHERE a.solicitante.publicId = :publicId")
    List<Agendamento> findBySolicitantePublicId(@Param("publicId") UUID publicId);

    @Query("SELECT a FROM Agendamento a WHERE a.paciente.publicId = :publicId")
    List<Agendamento> findByPacientePublicId(@Param("publicId") UUID publicId);
}