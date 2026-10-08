package com.hospital.agendamento_api.repository;

import com.hospital.agendamento_api.entity.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // Trava de regra de negócio: verifica se o paciente já tem pendência para o exame
    boolean existsByPacienteIdAndExameIdAndStatus(Long pacienteId, Long exameId, String status);

    @Query("SELECT a FROM Agendamento a WHERE a.solicitante.publicId = :publicId")
    List<Agendamento> findBySolicitantePublicId(@Param("publicId") UUID publicId);

    @Query("SELECT a FROM Agendamento a WHERE a.paciente.publicId = :publicId")
    List<Agendamento> findByPacientePublicId(@Param("publicId") UUID publicId);

    Optional<Agendamento> findByPublicId(UUID publicId);

    @Query("SELECT a FROM Agendamento a WHERE a.publicId = :publicId AND a.solicitante.publicId = :solicitantePublicId")
    Optional<Agendamento> findByPublicIdAndSolicitantePublicId(
            @Param("publicId") UUID publicId, 
            @Param("solicitantePublicId") UUID solicitantePublicId
    );
}